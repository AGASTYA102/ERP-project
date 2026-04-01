package com.erp.manufacturing;
 
import com.erp.manufacturing.entity.*;
import com.erp.manufacturing.enums.MaterialType;
import com.erp.manufacturing.enums.OrderStatus;
import com.erp.manufacturing.repository.*;
import com.erp.manufacturing.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
 
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
 
@SpringBootTest
@ActiveProfiles("test")
public class RigorousWorkflowSimulationTest {
 
    @Autowired private OrderService orderService;
    @Autowired private DesignService designService;
    @Autowired private PurchaseService purchaseService;
    @Autowired private ProductionService productionService;
    @Autowired private AccountsService accountsService;
    
    @Autowired private OrderRepository orderRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private OrderLogRepository orderLogRepository;
 
    @SpyBean private OrderLogService orderLogService;
 
    private Long testClientId;
    private Long testProductId;
 
    @BeforeEach
    public void setup() {
        orderLogRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll(); // Must delete products before clients due to FK
        clientRepository.deleteAll();
        stockRepository.deleteAll();
 
        Client client = clientRepository.save(Client.builder().name("Simulation Client").build());
        testClientId = client.getId();
 
        Product product = productRepository.save(Product.builder()
                .name("Simulation Product")
                .client(client)
                .build());
        testProductId = product.getId();
 
        stockRepository.save(Stock.builder()
                .materialName("Raw Material")
                .type(MaterialType.REEL)
                .quantity(1000.0)
                .build());
    }
 
    @Test
    public void fullStateTransitionSequenceWithLogContent() {
        String user = "agast";
        
        // 1. Create Order
        OrderEntity order = new OrderEntity();
        order.setClient(Client.builder().id(testClientId).build());
        order.setProduct(Product.builder().id(testProductId).build());
        order.setQuantity(100);
        
        OrderEntity savedOrder = orderService.createOrder(order, user);
        Long orderId = savedOrder.getId();
        
        assertEquals(OrderStatus.DESIGN_PENDING, savedOrder.getStatus());
        verifyLogs(orderId, 2, "Status changed to DESIGN_PENDING", user);
 
        // 2. Submit Design
        Design design = new Design();
        design.setDesignFilePath("/design/path.pdf");
        designService.submitDesign(design, orderId, "designer_user");
        
        OrderEntity afterDesign = orderRepository.findById(orderId).get();
        assertEquals(OrderStatus.PURCHASE_PENDING, afterDesign.getStatus());
        verifyLogs(afterDesign.getId(), 4, "Sent to Purchase Department", "designer_user");
 
        // 3. Process Purchase
        purchaseService.checkAndProcessPurchase(orderId, "Raw Material", 50.0, "purchase_user");
        OrderEntity afterPurchase = orderRepository.findById(orderId).get();
        assertEquals(OrderStatus.READY_FOR_PRODUCTION, afterPurchase.getStatus());
        verifyLogs(orderId, 5, "Stock verified and deducted", "purchase_user");
 
        // 4. Production (Partial then Complete)
        Production p = new Production();
        p.setPrintingDone(true);
        productionService.updateProduction(p, orderId, "prod_user");
        assertEquals(OrderStatus.IN_PRODUCTION, orderRepository.findById(orderId).get().getStatus());
 
        p.setCorrugationDone(true);
        p.setQuantityProduced(100);
        p.setTruckNumber("TRK-777");
        productionService.updateProduction(p, orderId, "prod_user");
        assertEquals(OrderStatus.COMPLETED, orderRepository.findById(orderId).get().getStatus());
        verifyLogs(orderId, 7, "Production completed", "prod_user");
 
        // 5. Finalize Accounts
        Accounts accounts = new Accounts();
        accounts.setRate(new BigDecimal("10.0"));
        accounts.setBillNumber("BILL-001");
        accounts.setPaymentStatus("PAID");
        accountsService.finalizeOrder(accounts, orderId, "accounts_user");
        
        OrderEntity finalOrder = orderRepository.findById(orderId).get();
        assertEquals(OrderStatus.COMPLETED, finalOrder.getStatus());
        verifyLogs(orderId, 8, "Bill Generated", "accounts_user");
    }
 
    @Test
    public void negativeTransitionShouldThrowException() {
        OrderEntity order = orderService.createOrder(
            OrderEntity.builder()
                .client(Client.builder().id(testClientId).build())
                .product(Product.builder().id(testProductId).build())
                .quantity(100)
                .build(), 
            "user"
        );
        
        // Assert CREATED -> DESIGN_PENDING (Auto transition in createOrder)
        assertEquals(OrderStatus.DESIGN_PENDING, order.getStatus());
 
        // Try illegal jump: DESIGN_PENDING -> COMPLETED
        assertThrows(IllegalStateException.class, () -> {
            orderService.updateOrderStatus(order.getId(), OrderStatus.COMPLETED, "Illegal Jump", "hacker");
        });
    }
 
    @Test
    public void transactionalRollbackOnFailure() {
        OrderEntity order = orderService.createOrder(
            OrderEntity.builder()
                .client(Client.builder().id(testClientId).build())
                .product(Product.builder().id(testProductId).build())
                .quantity(100)
                .build(), 
            "user"
        );
        
        Long orderId = order.getId();
        OrderStatus stateBefore = order.getStatus();
 
        // Mock log service to fail on the next transition
        doThrow(new RuntimeException("Audit System Offline"))
            .when(orderLogService).logAction(any(), anyString(), eq("failing_user"));
 
        // Attempt transition
        assertThrows(RuntimeException.class, () -> {
            orderService.updateOrderStatus(orderId, OrderStatus.PURCHASE_PENDING, "Should fail", "failing_user");
        });
 
        // Verify state is NOT changed (Rollback)
        OrderEntity freshOrder = orderRepository.findById(orderId).get();
        assertEquals(stateBefore, freshOrder.getStatus());
    }
 
    @Test
    public void optimisticLockingProtection() throws InterruptedException, ExecutionException {
        OrderEntity order = orderService.createOrder(
            OrderEntity.builder()
                .client(Client.builder().id(testClientId).build())
                .product(Product.builder().id(testProductId).build())
                .quantity(100)
                .build(), 
            "user"
        );
        Long orderId = order.getId();
 
        // Simulate two threads getting the same version
        OrderEntity thread1View = orderRepository.findById(orderId).get();
        OrderEntity thread2View = orderRepository.findById(orderId).get();
        assertEquals(thread1View.getVersion(), thread2View.getVersion());
 
        // Thread 1 succeeds
        thread1View.setPoNumber("PO-T1");
        orderRepository.save(thread1View);
 
        // Thread 2 should fail with OptimisticLockingFailureException
        thread2View.setPoNumber("PO-T2");
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            orderRepository.save(thread2View);
        });
    }
 
    private void verifyLogs(Long orderId, int expectedCount, String actionKeyword, String user) {
        List<OrderLog> logs = orderLogRepository.findByOrderIdOrderByTimestampDesc(orderId);
        // Sometimes multiple logs are created in one step (like createOrder)
        assertTrue(logs.size() >= expectedCount, "Expected at least " + expectedCount + " logs but found " + logs.size());
        
        boolean foundAction = logs.stream().anyMatch(l -> l.getAction().contains(actionKeyword));
        assertTrue(foundAction, "Could not find log containing keyword: " + actionKeyword);
        
        boolean correctUser = logs.stream().anyMatch(l -> l.getPerformedBy().equalsIgnoreCase(user));
        assertTrue(correctUser, "Could not find log performed by user: " + user);
    }
}

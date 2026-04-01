package com.erp.manufacturing;
 
import com.erp.manufacturing.entity.Client;
import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.entity.Product;
import com.erp.manufacturing.entity.Stock;
import com.erp.manufacturing.enums.MaterialType;
import com.erp.manufacturing.enums.OrderStatus;
import com.erp.manufacturing.repository.*;
import com.erp.manufacturing.service.OrderService;
import com.erp.manufacturing.service.PurchaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
 
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
 
import static org.junit.jupiter.api.Assertions.*;
 
@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyIntegrityTest {
 
    @Autowired
    private PurchaseService purchaseService;
 
    @Autowired
    private OrderService orderService;
 
    @Autowired
    private StockRepository stockRepository;
 
    @Autowired
    private OrderRepository orderRepository;
 
    @Autowired
    private ClientRepository clientRepository;
 
    @Autowired
    private ProductRepository productRepository;
 
    @Autowired
    private OrderLogRepository orderLogRepository;
 
    private Long testClientId;
    private Long testProductId;
 
    @BeforeEach
    public void setup() {
        orderLogRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll(); // Must delete products before clients due to FK
        clientRepository.deleteAll();
        stockRepository.deleteAll();
 
        Client client = clientRepository.save(Client.builder().name("Concurrency Client").build());
        testClientId = client.getId();
 
        Product product = productRepository.save(Product.builder()
                .name("Concurrency Product")
                .client(client) // Mandatory relationship
                .build());
        testProductId = product.getId();
    }
 
    @Test
    public void parallelStockDeductionShouldBeAtomic() throws InterruptedException, ExecutionException {
        // 1. Prepare Stock (100 units)
        String material = "Atomic Paper";
        stockRepository.save(Stock.builder()
                .materialName(material)
                .type(MaterialType.REEL)
                .quantity(100.0)
                .build());
 
        // 2. Prepare 10 Orders in PURCHASE_PENDING
        List<Long> orderIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            OrderEntity order = new OrderEntity();
            order.setClient(Client.builder().id(testClientId).build());
            order.setProduct(Product.builder().id(testProductId).build());
            order.setQuantity(100);
            order.setStatus(OrderStatus.PURCHASE_PENDING);
            orderIds.add(orderRepository.save(order).getId());
        }
 
        // 3. Launch 10 threads to deduct 10 units each
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
 
        for (int i = 0; i < threadCount; i++) {
            Long orderId = orderIds.get(i);
            futures.add(executor.submit(() -> {
                try {
                    latch.await(); // Wait for signal to start all at once
                    purchaseService.checkAndProcessPurchase(orderId, material, 10.0, "tester");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
 
        latch.countDown(); // Start!
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();
 
        // 4. Verify Final Stock is exactly 0
        // Use findAll to avoid the PESSIMISTIC_WRITE lock requirement on findByMaterialName during verification
        Stock finalStock = stockRepository.findAll().stream()
                .filter(s -> s.getMaterialName().equals(material))
                .findFirst()
                .orElseThrow();
        assertEquals(0.0, finalStock.getQuantity(), "Stock should be exactly 0 after 10 deductions of 10 units");
 
        // 5. Verify all orders transitioned to READY_FOR_PRODUCTION
        for (Long id : orderIds) {
            assertEquals(OrderStatus.READY_FOR_PRODUCTION, orderRepository.findById(id).get().getStatus());
        }
    }
 
    @Test
    public void optimisticLockingShouldPreventStaleUpdates() throws InterruptedException {
        // 1. Prepare Order
        OrderEntity order = new OrderEntity();
        order.setClient(Client.builder().id(testClientId).build());
        order.setProduct(Product.builder().id(testProductId).build());
        order.setQuantity(200);
        order.setStatus(OrderStatus.COMPLETED);
        Long orderId = orderRepository.save(order).getId();
 
        // 2. Launch 2 threads trying to update the same order status
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
 
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    orderService.updateOrderStatus(orderId, OrderStatus.CLOSED, "Finalizing", "concurrent-user");
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // Other errors (like WorkflowValidator if one finishes first)
                    // But with Optimistic Locking, the second one should fail on version before workflow if they overlap
                    failureCount.incrementAndGet();
                }
            });
        }
 
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
 
        // 3. Verify exactly one succeeded and one failed
        assertEquals(1, successCount.get(), "Only one thread should succeed");
        assertEquals(1, failureCount.get(), "One thread should fail due to @Version conflict or status transition");
    }
}

package com.erp.manufacturing;

import com.erp.manufacturing.entity.Client;
import com.erp.manufacturing.entity.Product;
import com.erp.manufacturing.repository.ClientRepository;
import com.erp.manufacturing.repository.ProductRepository;
import com.erp.manufacturing.repository.StockRepository;
import com.erp.manufacturing.entity.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private StockRepository stockRepository;

    private Long testClientId;
    private Long testProductId;

    @BeforeEach
    public void setup() {
        Client client = new Client();
        client.setName("Test Client");
        client = clientRepository.save(client);
        testClientId = client.getId();

        Product product = new Product();
        product.setName("Test Product");
        product = productRepository.save(product);
        testProductId = product.getId();

        Stock stock = new Stock();
        stock.setMaterialName("Paper Roll");
        stock.setType(com.erp.manufacturing.enums.MaterialType.REEL);
        stock.setQuantity(500.0);
        stockRepository.save(stock);
    }

    @Test
    @WithMockUser(username = "gm", roles = {"GM"})
    public void testFullWorkflow() throws Exception {
        // Step 1: Create Order
        String response = mockMvc.perform(post("/test/workflow/create-order")
                .with(csrf())
                .param("clientId", testClientId.toString())
                .param("productId", testProductId.toString())
                .param("quantity", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DESIGN_PENDING"))
                .andReturn().getResponse().getContentAsString();
        
        // Extract orderId (basic string manipulation for simplicity in test)
        String orderIdStr = response.split("\"orderId\":")[1].split(",")[0];
        
        // Step 2: Submit Design
        mockMvc.perform(post("/test/workflow/submit-design")
                .with(csrf())
                .param("orderId", orderIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PURCHASE_PENDING"));

        // Negative Test: Try design again (should fail)
        mockMvc.perform(post("/test/workflow/submit-design")
                .with(csrf())
                .param("orderId", orderIdStr))
                .andExpect(status().isBadRequest()); // Or 500 depending on exception handler

        // Step 3: Purchase (Stock available)
        mockMvc.perform(post("/test/workflow/process-purchase")
                .with(csrf())
                .param("orderId", orderIdStr)
                .param("materialName", "Paper Roll")
                .param("requiredQty", "100.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_PRODUCTION"));

        // Step 4: Production (Start then Complete)
        mockMvc.perform(post("/test/workflow/update-production")
                .with(csrf())
                .param("orderId", orderIdStr)
                .param("isComplete", "false")
                .param("quantityProduced", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PRODUCTION"));

        mockMvc.perform(post("/test/workflow/update-production")
                .with(csrf())
                .param("orderId", orderIdStr)
                .param("isComplete", "true")
                .param("quantityProduced", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Step 5: Accounts finalize
        mockMvc.perform(post("/test/workflow/finalize-accounts")
                .with(csrf())
                .param("orderId", orderIdStr)
                .param("rate", "15.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }
}

package com.erp.manufacturing;
 
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
 
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SystemRobustnessTest {
 
    @Autowired
    private MockMvc mockMvc;
 
    @Test
    @WithMockUser(roles = "GENERAL_MANAGER")
    public void invalidOrderIdShouldReturnCleanError() throws Exception {
        // Mapping is /gm/orders/{id}, not /gm/order-details/{id}
        mockMvc.perform(get("/gm/orders/999999"))
                .andExpect(status().isOk()) 
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(content().string(not(containsString("StackTraceElement"))))
                .andExpect(content().string(not(containsString("at com.erp.manufacturing"))));
    }
 
    @Test
    @WithMockUser(roles = "GENERAL_MANAGER")
    public void restRequestShouldReturnJsonError() throws Exception {
        // Ensure POST is used
        mockMvc.perform(post("/test/workflow/submit-design")
                .with(csrf())
                .param("orderId", "999999")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Order not found")));
    }
 
    @Test
    @WithMockUser(roles = "DESIGNER")
    public void invalidWorkflowStepShouldBeHandled() throws Exception {
        // Trying to submit design via REST for a non-existent order (which triggers EntityNotFound in service)
        mockMvc.perform(post("/test/workflow/submit-design")
                .with(csrf())
                .param("orderId", "888888")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Order not found")));
    }
 
    @Test
    @WithMockUser(roles = "GENERAL_MANAGER")
    public void missingDataInPostShouldReturnBadRequest() throws Exception {
        // Attempt to create client with missing data via a POST that validates
        // Note: We need a controller endpoint that uses @Valid or triggers ConstraintViolation
        // For this simulation, we'll hit the /gm/create-client (if exists) or create-order with nulls
        
        mockMvc.perform(post("/test/workflow/create-order")
                .with(csrf())
                .param("clientId", "") // Missing ID
                .param("productId", "1")
                .param("quantity", "100")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}

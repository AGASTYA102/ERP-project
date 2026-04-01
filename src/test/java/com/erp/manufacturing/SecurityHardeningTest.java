package com.erp.manufacturing;
 
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
 
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
 
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityHardeningTest {
 
    @Autowired
    private MockMvc mockMvc;
 
    @Test
    public void unauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/gm"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
 
    @Test
    @WithMockUser(username = "designer", roles = {"DESIGNER"})
    public void forbiddenAccessForWrongRole() throws Exception {
        mockMvc.perform(get("/accounts"))
                .andExpect(status().isForbidden());
    }
 
    @Test
    @WithMockUser(username = "gm", roles = {"GENERAL_MANAGER"})
    public void csrfProtectionEnforcedOnPost() throws Exception {
        // Performing a POST without CSRF should fail with 403 Forbidden
        mockMvc.perform(post("/gm/clients/save")
                .param("name", "Test Client"))
                .andExpect(status().isForbidden());
    }
 
    @Test
    @WithMockUser(username = "gm", roles = {"GENERAL_MANAGER"})
    public void csrfSuccessWithToken() throws Exception {
        // Performing a POST with CSRF should NOT return 403 (might return 302 or 200 depending on logic)
        mockMvc.perform(post("/gm/clients/save")
                .with(csrf())
                .param("name", "Test Client")
                .param("gstNo", "22AAAAA0000A1Z5") // Valid GST
                .param("address", "Test Address"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void h2ConsoleForbiddenInTestProfile() throws Exception {
        // H2 console should be forbidden or not found in non-dev profiles
        // It redirects to /login because it's an unauthenticated request to a protected resource
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}

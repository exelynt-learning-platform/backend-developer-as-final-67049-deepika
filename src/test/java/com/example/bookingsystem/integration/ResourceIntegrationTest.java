package com.example.bookingsystem.integration;

import com.example.bookingsystem.dto.ResourceRequest;
import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ResourceIntegrationTest extends AbstractIntegrationTest {

    @Test
    void listResources_asAuthenticatedUser_returnsOk() throws Exception {
        mockMvc.perform(get("/api/resources").header("Authorization", tokenFor("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void createResource_asAdmin_succeeds() throws Exception {
        ResourceRequest request = new ResourceRequest("New Room", "Desc", "ROOM", true);

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", tokenFor("admin"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Room"));
    }

    @Test
    void createResource_asUser_isForbidden() throws Exception {
        ResourceRequest request = new ResourceRequest("New Room", "Desc", "ROOM", true);

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", tokenFor("alice"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createResource_withBlankName_returns400() throws Exception {
        ResourceRequest request = new ResourceRequest("", "Desc", "ROOM", true);

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", tokenFor("admin"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void updateResource_asUser_isForbidden() throws Exception {
        ResourceRequest request = new ResourceRequest("Updated", "Desc", "ROOM", true);

        mockMvc.perform(put("/api/resources/" + testResource.getId())
                        .header("Authorization", tokenFor("alice"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteResource_asAdmin_succeeds() throws Exception {
        mockMvc.perform(delete("/api/resources/" + testResource.getId())
                        .header("Authorization", tokenFor("admin")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteResource_asUser_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/resources/" + testResource.getId())
                        .header("Authorization", tokenFor("alice")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getResource_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/resources/999999")
                        .header("Authorization", tokenFor("admin")))
                .andExpect(status().isNotFound());
    }
}

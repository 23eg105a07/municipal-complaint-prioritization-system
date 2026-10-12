package com.municipal.system.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.municipal.system.auth.dto.AuthResponse;
import com.municipal.system.auth.dto.RegisterRequest;
import com.municipal.system.complaint.dto.ComplaintResponse;
import com.municipal.system.complaint.dto.CreateComplaintRequest;
import com.municipal.system.complaint.dto.UpvoteResponse;
import com.municipal.system.complaint.entity.ComplaintCategory;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ComplaintFlowIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End citizen registration, complaint submission, upvote, and priority queue flow")
    void testComplaintEndToEndFlow() throws Exception {
        // 1. Register citizen 1
        RegisterRequest registerReq1 = new RegisterRequest(
                "citizen1@civic.test", "password123", "Jane Doe", "+1234567890"
        );
        MvcResult regResult1 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq1)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse auth1 = objectMapper.readValue(regResult1.getResponse().getContentAsString(), AuthResponse.class);
        String token1 = auth1.accessToken();

        // 2. Register citizen 2
        RegisterRequest registerReq2 = new RegisterRequest(
                "citizen2@civic.test", "password123", "John Smith", "+1987654321"
        );
        MvcResult regResult2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq2)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse auth2 = objectMapper.readValue(regResult2.getResponse().getContentAsString(), AuthResponse.class);
        String token2 = auth2.accessToken();

        // 3. Citizen 1 submits a high-severity critical pothole complaint (severity 5)
        CreateComplaintRequest complaintReq = new CreateComplaintRequest(
                "Critical sinkhole on Main Street",
                "Large hazardous sinkhole in the center lane of Main St.",
                ComplaintCategory.POTHOLE,
                5,
                1,
                new BigDecimal("37.774929"),
                new BigDecimal("-122.419416"),
                "100 Main Street",
                "https://storage.civic.test/sinkhole.jpg"
        );

        MvcResult createResult = mockMvc.perform(post("/api/complaints")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(complaintReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.departmentCode").value("ROADS"))
                .andExpect(jsonPath("$.affectedCount").value(1))
                .andReturn();

        ComplaintResponse createdComplaint = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ComplaintResponse.class
        );
        Long complaintId = createdComplaint.id();

        // Severity 5 receives instant +50 critical boost: score should be > 50
        assertThat(createdComplaint.priorityScore()).isGreaterThan(new BigDecimal("50.0"));

        // 4. Citizen 2 upvotes the complaint
        MvcResult upvoteResult = mockMvc.perform(post("/api/complaints/" + complaintId + "/upvote")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvoted").value(true))
                .andExpect(jsonPath("$.affectedCount").value(2))
                .andReturn();

        UpvoteResponse upvoteResponse = objectMapper.readValue(
                upvoteResult.getResponse().getContentAsString(), UpvoteResponse.class
        );
        assertThat(upvoteResponse.affectedCount()).isEqualTo(2);

        // 5. Verify the priority queue contains the ticket
        mockMvc.perform(get("/api/complaints/priority")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(complaintId))
                .andExpect(jsonPath("$.content[0].affectedCount").value(2));

        // 6. Check status audit history
        mockMvc.perform(get("/api/complaints/" + complaintId + "/history")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].toStatus").value("REPORTED"));
    }
}

package com.project.goal_tracker.test_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.goal_tracker.dto.ProgressCreate;
import com.project.goal_tracker.dto.ProgressUpdate;
import com.project.goal_tracker.model.CustomUserDetails;
import com.project.goal_tracker.model.Goal;
import com.project.goal_tracker.model.Progress;
import com.project.goal_tracker.model.User;
import com.project.goal_tracker.repository.GoalRepository;
import com.project.goal_tracker.repository.ProgressRepository;
import com.project.goal_tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TestProgressApi {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private GoalRepository goalRepository;

    @MockBean
    private ProgressRepository progressRepository;

    private User user;
    private Goal goal;
    private Progress progress;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        goal = new Goal();
        goal.setId(1L);
        goal.setUser(user);

        progress = new Progress();
        progress.setId(1L);
        progress.setGoal(goal);

        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(user);
        Mockito.when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        Mockito.when(progressRepository.findById(1L)).thenReturn(Optional.of(progress));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createProgress_shouldReturn201() throws Exception {
        ProgressCreate request = new ProgressCreate();
        request.setAmount(10.0);
        request.setUpdateNote("Weekly update");

        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        MvcResult result = mockMvc.perform(post("/goals/1/progress")
                        .with(user(customUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        System.out.println("POST /goals/1/progress");
        System.out.println("Status: " + result.getResponse().getStatus());
        System.out.println("Body: " + result.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void listProgress_shouldReturn200() throws Exception {
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        MvcResult result = mockMvc.perform(get("/goals/1/progress")
                        .with(user(customUserDetails)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("GET /goals/1/progress");
        System.out.println("Status: " + result.getResponse().getStatus());
        System.out.println("Body: " + result.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void retrieveProgress_shouldReturn200() throws Exception {
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        MvcResult result = mockMvc.perform(get("/goals/1/progress/1")
                        .with(user(customUserDetails)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("GET /goals/1/progress/1");
        System.out.println("Status: " + result.getResponse().getStatus());
        System.out.println("Body: " + result.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void updateProgress_shouldReturn200() throws Exception {
        ProgressUpdate update = new ProgressUpdate();
        update.setAmount(20.0);
        update.setUpdateNote("Updated progress");

        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        MvcResult result = mockMvc.perform(patch("/goals/1/progress/1")
                        .with(user(customUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("PATCH /goals/1/progress/1");
        System.out.println("Status: " + result.getResponse().getStatus());
        System.out.println("Body: " + result.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteProgress_shouldReturn200() throws Exception {
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        MvcResult result = mockMvc.perform(delete("/goals/1/progress/1")
                        .with(user(customUserDetails)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("DELETE /goals/1/progress/1");
        System.out.println("Status: " + result.getResponse().getStatus());
        System.out.println("Body: " + result.getResponse().getContentAsString());
    }
}

package com.project.goal_tracker.test_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.goal_tracker.dto.GoalCreate;
import com.project.goal_tracker.model.CustomUserDetails;
import com.project.goal_tracker.model.Goal;
import com.project.goal_tracker.model.User;
import com.project.goal_tracker.repository.GoalRepository;
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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;

import static com.project.goal_tracker.utils.CommonTestTools.debugLog;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;


import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TestGoalApi {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoalRepository goalRepository;

    @MockBean
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setup() {
        this.user = new User();
        this.user.setId(1L);
        this.user.setEmail("test@example.com");

        Mockito.when(userRepository.findByEmail("test@example.com"))
                .thenReturn(this.user);


    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createGoal_shouldReturn201_whenValid() throws Exception {
        GoalCreate request = new GoalCreate();
        request.setTitle("Read books");
        request.setDescription("Read 12 books in a year");
        request.setMetric("Books");
        request.setTotalDesiredAmount(12.0);

        CustomUserDetails customUserDetails = new CustomUserDetails(this.user);

        MvcResult result = mockMvc.perform(
                        post("/goals")
                                .with(user(customUserDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn();
        debugLog(result);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createGoal_shouldReturn400_whenMissingTitle() throws Exception {
        GoalCreate request = new GoalCreate();
        request.setDescription("Missing title test");
        request.setMetric("Pages");
        request.setTotalDesiredAmount(100.0);

        CustomUserDetails customUserDetails = new CustomUserDetails(this.user);

        MvcResult result = mockMvc.perform(
                        post("/goals")
                                .with(user(customUserDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        debugLog(result);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getGoals_shouldReturn200_andListGoals() throws Exception {
        // Prepare mock repository to return some goals
        Goal goal1 = new Goal();
        goal1.setId(1L);
        goal1.setTitle("Goal 1");
        goal1.setUser(this.user);

        Goal goal2 = new Goal();
        goal2.setId(2L);
        goal2.setTitle("Goal 2");
        goal2.setUser(this.user);

        Mockito.when(goalRepository.findByUserId(this.user.getId()))
                .thenReturn(List.of(goal1, goal2));

        CustomUserDetails customUserDetails = new CustomUserDetails(this.user);

        MvcResult result = mockMvc.perform(
                        get("/goals")
                                .with(user(customUserDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        debugLog(result);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteGoal_shouldReturn204_whenExists() throws Exception {
        Long goalId = 1L;

        Mockito.when(goalRepository.findById(goalId))
                .thenReturn(Optional.of(new Goal(){{
                    setId(goalId);
                    setUser(user);
                }}));

        Mockito.doNothing().when(goalRepository).deleteById(goalId);

        CustomUserDetails customUserDetails = new CustomUserDetails(this.user);

        MvcResult result = mockMvc.perform(
                        delete("/goals/{id}", goalId)
                                .with(user(customUserDetails))
                )
                .andExpect(status().isNoContent())
                .andReturn();

        debugLog(result);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteGoal_shouldReturn404_whenNotFound() throws Exception {
        Long goalId = 999L;

        Mockito.when(goalRepository.findById(goalId))
                .thenReturn(Optional.empty());

        CustomUserDetails customUserDetails = new CustomUserDetails(this.user);

        MvcResult result = mockMvc.perform(
                        delete("/goals/{id}", goalId)
                                .with(user(customUserDetails))
                )
                .andExpect(status().isNotFound())
                .andReturn();

        debugLog(result);
    }
}
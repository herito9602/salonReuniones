package com.example.meetingrooms.room;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsRooms() throws Exception {
        mockMvc.perform(createRoom("{\"name\":\"Orion\",\"capacity\":8}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Orion"))
                .andExpect(jsonPath("$.capacity").value(8));

        mockMvc.perform(get("/api/v1/rooms").with(user("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Orion"));
    }

    @Test
    void rejectsInvalidRoomWithFieldErrors() throws Exception {
        mockMvc.perform(createRoom("{\"name\":\" \",\"capacity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("capacity", "name")));
    }

    @Test
    void rejectsDuplicateRoomNameIgnoringCase() throws Exception {
        mockMvc.perform(createRoom("{\"name\":\"Vega\",\"capacity\":4}")).andExpect(status().isCreated());

        mockMvc.perform(createRoom("{\"name\":\"vega\",\"capacity\":6}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A room named 'vega' already exists"));
    }

    @Test
    void returnsNotFoundForUnknownRoom() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/999999").with(user("reader")))
                .andExpect(status().isNotFound());
    }

    private static MockHttpServletRequestBuilder createRoom(String body) {
        return post("/api/v1/rooms")
                .with(user("admin")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}

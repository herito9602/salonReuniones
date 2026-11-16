package com.example.meetingrooms.reservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationControllerIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private long roomId;

    @BeforeEach
    void createRoom() throws Exception {
        String body = mockMvc.perform(post("/api/v1/rooms").with(httpBasic(USERNAME, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Andromeda\",\"capacity\":10}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        roomId = objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void createsReservation() throws Exception {
        book("2099-03-10T10:00:00", "2099-03-10T11:00:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.roomName").value("Andromeda"))
                .andExpect(jsonPath("$.responsible").value("Grace Hopper"))
                .andExpect(jsonPath("$.start").value("2099-03-10T10:00:00"))
                .andExpect(jsonPath("$.end").value("2099-03-10T11:00:00"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void returnsConflictWhenRoomIsAlreadyBooked() throws Exception {
        book("2099-03-10T10:00:00", "2099-03-10T11:00:00").andExpect(status().isCreated());

        book("2099-03-10T10:30:00", "2099-03-10T11:30:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void allowsBackToBackReservations() throws Exception {
        book("2099-03-10T10:00:00", "2099-03-10T11:00:00").andExpect(status().isCreated());
        book("2099-03-10T11:00:00", "2099-03-10T12:00:00").andExpect(status().isCreated());
    }

    @Test
    void cancellingReleasesTheSlot() throws Exception {
        String body = book("2099-03-10T10:00:00", "2099-03-10T11:00:00")
                .andReturn().getResponse().getContentAsString();
        long reservationId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", reservationId).with(httpBasic(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", reservationId).with(httpBasic(USERNAME, PASSWORD)))
                .andExpect(status().isConflict());

        book("2099-03-10T10:00:00", "2099-03-10T11:00:00").andExpect(status().isCreated());
    }

    @Test
    void rejectsMissingFieldsWithClearErrors() throws Exception {
        mockMvc.perform(post("/api/v1/reservations").with(httpBasic(USERNAME, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsible\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("end", "responsible", "roomId", "start")));
    }

    @Test
    void rejectsEndBeforeStart() throws Exception {
        book("2099-03-10T11:00:00", "2099-03-10T10:00:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("end"))
                .andExpect(jsonPath("$.errors[0].message").value("end must be after start"));
    }

    @Test
    void rejectsMalformedDate() throws Exception {
        book("tomorrow at ten", "2099-03-10T10:00:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }

    @Test
    void returnsNotFoundForUnknownRoom() throws Exception {
        roomId = 999_999;
        book("2099-03-10T10:00:00", "2099-03-10T11:00:00").andExpect(status().isNotFound());
    }

    @Test
    void listsReservationsPaginated() throws Exception {
        book("2099-03-10T10:00:00", "2099-03-10T11:00:00");
        book("2099-03-10T12:00:00", "2099-03-10T13:00:00");

        String body = mockMvc.perform(get("/api/v1/reservations").param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode page = objectMapper.readTree(body);

        assertThat(page.get("content")).hasSize(1);
        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        assertThat(page.get("size").asInt()).isEqualTo(1);
    }

    @Test
    void filtersByRoomAndDate() throws Exception {
        long andromeda = roomId;
        book("2099-03-10T10:00:00", "2099-03-10T11:00:00");
        book("2099-03-11T10:00:00", "2099-03-11T11:00:00");
        roomId = createRoom("Pegasus");
        book("2099-03-10T10:00:00", "2099-03-10T11:00:00");

        mockMvc.perform(get("/api/v1/reservations")
                        .param("roomId", String.valueOf(andromeda))
                        .param("date", "2099-03-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].roomName").value("Andromeda"))
                .andExpect(jsonPath("$.content[0].start").value("2099-03-10T10:00:00"));

        mockMvc.perform(get("/api/v1/reservations").param("date", "2099-03-10"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/reservations").param("roomId", String.valueOf(andromeda)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].start").value("2099-03-10T10:00:00"));
    }

    @Test
    void dateFilterIncludesReservationsSpanningMidnight() throws Exception {
        book("2099-03-10T23:00:00", "2099-03-11T01:00:00");

        mockMvc.perform(get("/api/v1/reservations").param("date", "2099-03-10"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/reservations").param("date", "2099-03-11"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/reservations").param("date", "2099-03-12"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsInvalidFilters() throws Exception {
        mockMvc.perform(get("/api/v1/reservations").param("date", "10/03/2099"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("date"));

        mockMvc.perform(get("/api/v1/reservations").param("roomId", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("roomId"));

        mockMvc.perform(get("/api/v1/reservations").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sort"));
    }

    private long createRoom(String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/rooms").with(httpBasic(USERNAME, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"capacity\":6}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private ResultActions book(String start, String end) throws Exception {
        String json = """
                {"roomId": %d, "responsible": "Grace Hopper", "start": "%s", "end": "%s"}
                """.formatted(roomId, start, end);
        return mockMvc.perform(post("/api/v1/reservations").with(httpBasic(USERNAME, PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}

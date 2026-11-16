package com.example.meetingrooms.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    private static final String ROOM_JSON = "{\"name\":\"Lyra\",\"capacity\":4}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void readEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/api/v1/rooms")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reservations")).andExpect(status().isOk());
    }

    @Test
    void writeEndpointsRequireCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/rooms").contentType(MediaType.APPLICATION_JSON).content(ROOM_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"meeting-rooms\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unauthorized"));

        mockMvc.perform(post("/api/v1/reservations").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/reservations/1/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/rooms").with(httpBasic("admin", "wrong"))
                        .contentType(MediaType.APPLICATION_JSON).content(ROOM_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsValidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/rooms").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(ROOM_JSON))
                .andExpect(status().isCreated());
    }
}

package com.vidal.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class QuizSubmitterTest {

    @Test
    void postsExpectedPayloadToSubmitEndpoint() throws Exception {
        ObjectMapper json = new ObjectMapper();

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("OK"));
            server.start();

            String endpointBase = server.url("/srm-quiz-task").toString();

            OkHttpClient http = new OkHttpClient();
            QuizSubmitter submitter = new QuizSubmitter(http, json);

            List<LeaderboardRow> leaderboard = List.of(
                    new LeaderboardRow("Alice", 15),
                    new LeaderboardRow("Bob", 7)
            );

            String response = submitter.submit(endpointBase, "2024CS101", leaderboard);
            assertEquals("OK", response);

            var request = server.takeRequest();
            assertEquals("POST", request.getMethod());
            assertEquals("/srm-quiz-task/quiz/submit", request.getPath());
            assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"));

            JsonNode body = json.readTree(request.getBody().readUtf8());
            assertEquals("2024CS101", body.get("regNo").asText());

            JsonNode rows = body.get("leaderboard");
            assertNotNull(rows);
            assertTrue(rows.isArray());
            assertEquals(2, rows.size());
            assertEquals("Alice", rows.get(0).get("participant").asText());
            assertEquals(15, rows.get(0).get("totalScore").asInt());
        }
    }
}

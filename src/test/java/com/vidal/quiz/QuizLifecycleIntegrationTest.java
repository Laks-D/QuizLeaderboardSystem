package com.vidal.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

final class QuizLifecycleIntegrationTest {

    @Test
    void liveDryRunWritesAuditAndExports() throws Exception {
        ObjectMapper json = new ObjectMapper();

        try (MockWebServer server = new MockWebServer()) {
            // Enqueue 10 poll responses.
            for (int i = 0; i < 10; i++) {
                String body = "{\"events\":[" +
                        "{\"roundId\":\"r" + i + "\",\"participant\":\"Alice\",\"score\":1}," +
                        "{\"roundId\":\"r" + i + "\",\"participant\":\"Alice\",\"score\":1}," +
                        "{\"roundId\":\"r" + i + "\",\"participant\":\"Bob\",\"score\":2}" +
                        "]}";
                server.enqueue(new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(body));
            }

            server.start();

            String endpointBase = server.url("/srm-quiz-task").toString();
                Path outDir = Paths.get("")
                    .toAbsolutePath()
                    .normalize()
                    .resolve("target")
                    .resolve("demo-out");

            AppConfig config = new AppConfig(
                    "2024CS101",
                    endpointBase,
                    RunMode.LIVE,
                    outDir,
                    true
            );

            // No sleep in tests.
            new QuizLifecycle(Duration.ZERO, 10).run(config);

            assertTrue(Files.exists(outDir.resolve("leaderboard.json")));
            assertTrue(Files.exists(outDir.resolve("leaderboard.csv")));
            assertTrue(Files.exists(outDir.resolve("summary.json")));

            for (int i = 0; i < 10; i++) {
                assertTrue(Files.exists(outDir.resolve("raw").resolve("poll-" + i + ".json")));
            }

            String leaderboardRaw = Files.readString(outDir.resolve("leaderboard.json"), StandardCharsets.UTF_8);
            JsonNode leaderboard = json.readTree(leaderboardRaw);
            assertEquals("2024CS101", leaderboard.get("regNo").asText());
            assertEquals(2, leaderboard.get("leaderboard").size());
            assertEquals("Bob", leaderboard.get("leaderboard").get(0).get("participant").asText());
            assertEquals(20, leaderboard.get("leaderboard").get(0).get("totalScore").asInt());
        }
    }
}

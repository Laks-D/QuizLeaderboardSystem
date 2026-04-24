package com.vidal.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class QuizLifecycle {
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(5);
    private static final int DEFAULT_POLL_COUNT = 10;

    private final Duration pollInterval;
    private final int pollCount;

    private final ObjectMapper json = new ObjectMapper();

    QuizLifecycle() {
        this(DEFAULT_POLL_INTERVAL, DEFAULT_POLL_COUNT);
    }

    QuizLifecycle(Duration pollInterval, int pollCount) {
        if (pollInterval == null) {
            throw new IllegalArgumentException("pollInterval is required");
        }
        if (pollCount <= 0) {
            throw new IllegalArgumentException("pollCount must be > 0");
        }
        this.pollInterval = pollInterval;
        this.pollCount = pollCount;
    }

    void run(AppConfig config) throws Exception {
        System.out.println(">>> System Online. Targeted Registration: " + config.regNo());

        var http = new okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(Duration.ofSeconds(30))
            .build();

        try {
            Files.createDirectories(config.outDir());

            QuizApiClient api = new QuizApiClient(config.endpointBase(), json, http);
            QuizAggregator aggregator = new QuizAggregator();
            QuizExporter exporter = new QuizExporter(json);
            QuizSubmitter submitter = new QuizSubmitter(http, json);

            Path rawDir = config.outDir().resolve("raw");

            Instant started = Instant.now();
            int[] eventsPerPoll = new int[pollCount];

            if (config.mode() == RunMode.LIVE) {
                for (int pollIdx = 0; pollIdx < pollCount; pollIdx++) {
                    System.out.printf("[%d/%d] Ingesting data stream...%n", pollIdx, pollCount - 1);

                    QuizApiClient.PollFetch poll = api.fetchPoll(config.regNo(), pollIdx);
                    exporter.writePollRaw(rawDir, pollIdx, poll.rawJson());

                    eventsPerPoll[pollIdx] = poll.events().size();
                    aggregator.accept(poll.events());

                    if (pollIdx < pollCount - 1 && !pollInterval.isZero()) {
                        TimeUnit.MILLISECONDS.sleep(pollInterval.toMillis());
                    }
                }
            } else {
                for (int pollIdx = 0; pollIdx < pollCount; pollIdx++) {
                    Path pollFile = rawDir.resolve("poll-" + pollIdx + ".json");
                    if (!Files.exists(pollFile)) {
                        throw new IllegalStateException("Missing replay file: " + pollFile);
                    }

                    String raw = Files.readString(pollFile, StandardCharsets.UTF_8);
                    List<QuizEvent> events = QuizApiClient.parseEvents(json, pollIdx, raw);
                    eventsPerPoll[pollIdx] = events.size();
                    aggregator.accept(events);
                }
            }

            List<LeaderboardRow> leaderboard = aggregator.leaderboard();

            int totalScore = leaderboard.stream().mapToInt(LeaderboardRow::totalScore).sum();
            System.out.println(">>> Total Score: " + totalScore);

            exporter.writeLeaderboard(config.outDir(), config.regNo(), leaderboard);

            Map<String, Object> summary = new HashMap<>();
            summary.put("regNo", config.regNo());
            summary.put("mode", config.mode().name().toLowerCase());
            summary.put("pollCount", pollCount);
            summary.put("pollIntervalSeconds", pollInterval.toSeconds());
            summary.put("eventsSeen", aggregator.eventsSeen());
            summary.put("eventsAccepted", aggregator.eventsAccepted());
            summary.put("duplicatesSkipped", aggregator.duplicatesSkipped());
            summary.put("participants", aggregator.participants());
            summary.put("totalScore", totalScore);
            summary.put("eventsPerPoll", eventsPerPoll);
            summary.put("durationMs", Duration.between(started, Instant.now()).toMillis());

            exporter.writeSummary(config.outDir(), summary);

            if (config.dryRun()) {
                System.out.println(">>> Dry run enabled: skipping submission.");
                return;
            }

            submitFinalMetrics(submitter, config, leaderboard);
        } finally {
            try {
                http.dispatcher().executorService().shutdown();
            } catch (Exception ignored) {
            }
            try {
                http.connectionPool().evictAll();
            } catch (Exception ignored) {
            }
            try {
                if (http.cache() != null) {
                    http.cache().close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void submitFinalMetrics(QuizSubmitter submitter, AppConfig config, List<LeaderboardRow> leaderboard) throws IOException {
        System.out.println(">>> Transmitting final leaderboard to validator...");
        String response = submitter.submit(config.endpointBase(), config.regNo(), leaderboard);
        System.out.println("Validator Response: " + (response.isBlank() ? "<empty>" : response));
    }
}

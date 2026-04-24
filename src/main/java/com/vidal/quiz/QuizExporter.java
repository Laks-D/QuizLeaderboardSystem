package com.vidal.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class QuizExporter {
    private final ObjectMapper json;

    QuizExporter(ObjectMapper json) {
        this.json = json;
    }

    void writePollRaw(Path rawDir, int pollIndex, String rawJson) throws IOException {
        Files.createDirectories(rawDir);
        Path target = rawDir.resolve("poll-" + pollIndex + ".json");
        Files.writeString(target, rawJson, StandardCharsets.UTF_8);
    }

    void writeLeaderboard(Path outDir, String regNo, List<LeaderboardRow> leaderboard) throws IOException {
        Files.createDirectories(outDir);

        Map<String, Object> submissionShape = new HashMap<>();
        submissionShape.put("regNo", regNo);
        submissionShape.put("leaderboard", leaderboard.stream()
                .map(r -> Map.of("participant", r.participant(), "totalScore", r.totalScore()))
                .toList());

        Path jsonTarget = outDir.resolve("leaderboard.json");
        Files.writeString(jsonTarget,
                json.writerWithDefaultPrettyPrinter().writeValueAsString(submissionShape),
                StandardCharsets.UTF_8);

        Path csvTarget = outDir.resolve("leaderboard.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csvTarget, StandardCharsets.UTF_8)) {
            writer.write("participant,totalScore");
            writer.newLine();
            for (LeaderboardRow row : leaderboard) {
                writer.write(escapeCsv(row.participant()));
                writer.write(',');
                writer.write(Integer.toString(row.totalScore()));
                writer.newLine();
            }
        }
    }

    void writeSummary(Path outDir, Map<String, Object> summary) throws IOException {
        Files.createDirectories(outDir);
        summary.putIfAbsent("generatedAt", Instant.now().toString());
        Path target = outDir.resolve("summary.json");
        Files.writeString(target,
                json.writerWithDefaultPrettyPrinter().writeValueAsString(summary),
                StandardCharsets.UTF_8);
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}

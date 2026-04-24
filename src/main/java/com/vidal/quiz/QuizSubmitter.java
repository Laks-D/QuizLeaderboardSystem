package com.vidal.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class QuizSubmitter {
    private final OkHttpClient client;
    private final ObjectMapper json;

    QuizSubmitter(OkHttpClient client, ObjectMapper json) {
        this.client = Objects.requireNonNull(client, "client");
        this.json = Objects.requireNonNull(json, "json");
    }

    String submit(String endpointBase, String regNo, List<LeaderboardRow> leaderboard) throws IOException {
        String base = normalizeBase(endpointBase);

        Map<String, Object> submissionBody = new HashMap<>();
        submissionBody.put("regNo", regNo);
        submissionBody.put("leaderboard", leaderboard.stream()
                .map(r -> Map.of("participant", r.participant(), "totalScore", r.totalScore()))
                .toList());

        String payload = json.writerWithDefaultPrettyPrinter().writeValueAsString(submissionBody);
        RequestBody body = RequestBody.create(payload, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(base + "/quiz/submit").post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            return response.body().string();
        }
    }

    private static String normalizeBase(String endpointBase) {
        if (endpointBase == null) {
            throw new IllegalArgumentException("endpointBase is required");
        }
        return endpointBase.endsWith("/")
                ? endpointBase.substring(0, endpointBase.length() - 1)
                : endpointBase;
    }
}

package com.vidal.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

final class QuizApiClient {
    private final String endpointBase;
    private final OkHttpClient client;
    private final ObjectMapper json;

    QuizApiClient(String endpointBase, ObjectMapper json, OkHttpClient client) {
        this.endpointBase = Objects.requireNonNull(endpointBase, "endpointBase");
        this.json = Objects.requireNonNull(json, "json");
        this.client = Objects.requireNonNull(client, "client");
    }

    PollFetch fetchPoll(String regNo, int pollIndex) throws IOException {
        HttpUrl url = HttpUrl.parse(endpointBase + "/quiz/messages").newBuilder()
                .addQueryParameter("regNo", regNo)
                .addQueryParameter("poll", String.valueOf(pollIndex))
                .build();

        Request request = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " calling " + url);
            }

            if (response.body() == null) {
                throw new IOException("Empty response body from " + url);
            }

            String rawJson = response.body().string();
            List<QuizEvent> events = parseEvents(json, pollIndex, rawJson);
            return new PollFetch(pollIndex, rawJson, events);
        }
    }

    static List<QuizEvent> parseEvents(ObjectMapper json, int pollIndex, String rawJson) throws IOException {
        JsonNode root = json.readTree(rawJson);
        JsonNode eventsNode = root.get("events");

        List<QuizEvent> events = new ArrayList<>();
        if (eventsNode != null && eventsNode.isArray()) {
            for (JsonNode event : eventsNode) {
                JsonNode round = event.get("roundId");
                JsonNode participant = event.get("participant");
                JsonNode score = event.get("score");

                if (round == null || participant == null || score == null) {
                    continue;
                }

                events.add(new QuizEvent(
                        pollIndex,
                        round.asText(),
                        participant.asText(),
                        score.asInt()
                ));
            }
        }

        return events;
    }

    record PollFetch(int pollIndex, String rawJson, List<QuizEvent> events) {
    }
}

package com.vidal.quiz;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class QuizAggregator {
    private final DeduplicationLayer deduplication = new DeduplicationLayer();
    private final Map<String, Integer> scoreByParticipant = new HashMap<>();

    private int eventsSeen;
    private int eventsAccepted;
    private int duplicatesSkipped;

    void accept(List<QuizEvent> events) {
        for (QuizEvent event : events) {
            eventsSeen++;

            if (!deduplication.isFirstTime(event.roundId(), event.participant())) {
                duplicatesSkipped++;
                continue;
            }

            scoreByParticipant.merge(event.participant(), event.score(), Integer::sum);
            eventsAccepted++;
        }
    }

    List<LeaderboardRow> leaderboard() {
        List<LeaderboardRow> rows = new ArrayList<>(scoreByParticipant.size());
        for (Map.Entry<String, Integer> entry : scoreByParticipant.entrySet()) {
            rows.add(new LeaderboardRow(entry.getKey(), entry.getValue()));
        }

        rows.sort(Comparator.comparingInt(LeaderboardRow::totalScore).reversed());

        return rows;
    }

    int eventsSeen() {
        return eventsSeen;
    }

    int eventsAccepted() {
        return eventsAccepted;
    }

    int duplicatesSkipped() {
        return duplicatesSkipped;
    }

    int participants() {
        return scoreByParticipant.size();
    }
}

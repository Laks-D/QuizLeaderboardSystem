package com.vidal.quiz;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class QuizAggregatorTest {

    @Test
    void dedupesByRoundAndParticipantAndAccumulatesScores() {
        QuizAggregator aggregator = new QuizAggregator();

        List<QuizEvent> events = List.of(
                new QuizEvent(0, "r1", "Alice", 10),
                new QuizEvent(0, "r1", "Alice", 10),
                new QuizEvent(0, "r1", "Bob", 7),
                new QuizEvent(0, "r2", "Alice", 5)
        );

        aggregator.accept(events);

        assertEquals(4, aggregator.eventsSeen());
        assertEquals(3, aggregator.eventsAccepted());
        assertEquals(1, aggregator.duplicatesSkipped());
        assertEquals(2, aggregator.participants());

        List<LeaderboardRow> board = aggregator.leaderboard();
        assertEquals(2, board.size());

        // Alice total = 10 + 5, Bob total = 7
        assertTrue(board.get(0).totalScore() >= board.get(1).totalScore());
        assertEquals("Alice", board.get(0).participant());
        assertEquals(15, board.get(0).totalScore());
        assertEquals("Bob", board.get(1).participant());
        assertEquals(7, board.get(1).totalScore());
    }
}

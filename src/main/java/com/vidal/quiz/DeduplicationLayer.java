package com.vidal.quiz;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks unique quiz events across all polls to provide idempotency.
 *
 * Uniqueness key: roundId + "_" + participant
 */
final class DeduplicationLayer {
    private final Set<String> seen = new HashSet<>();

    boolean isFirstTime(String roundId, String participant) {
        String key = String.valueOf(roundId) + "_" + String.valueOf(participant);
        return seen.add(key);
    }
}

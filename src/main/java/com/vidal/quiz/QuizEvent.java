package com.vidal.quiz;

record QuizEvent(int pollIndex, String roundId, String participant, int score) {
}

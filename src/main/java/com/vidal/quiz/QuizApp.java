package com.vidal.quiz;

public class QuizApp {
    public static void main(String[] args) {
        try {
            AppConfig config = CliArgs.parse(args);
            new QuizLifecycle().run(config);
        } catch (Exception e) {
            System.err.println("Critical Failure: " + e.getMessage());
            System.err.println("Use --help for usage.");
            System.exit(1);
        }
    }
}
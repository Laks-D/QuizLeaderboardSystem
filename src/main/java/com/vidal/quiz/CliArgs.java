package com.vidal.quiz;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class CliArgs {
    private CliArgs() {
    }

    static AppConfig parse(String[] args) {
        Map<String, String> flags = new HashMap<>();
        boolean dryRun = false;

        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (token == null || token.isBlank()) {
                continue;
            }

            if ("--dryRun".equals(token)) {
                dryRun = true;
                continue;
            }

            if ("--help".equals(token) || "-h".equals(token)) {
                printUsageAndExit(0);
            }

            if (!token.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + token);
            }

            String key;
            String value;

            int eq = token.indexOf('=');
            if (eq >= 0) {
                key = token.substring(2, eq);
                value = token.substring(eq + 1);
            } else {
                key = token.substring(2);
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for --" + key);
                }
                value = args[++i];
            }

            flags.put(key, value);
        }

        String regNo = flags.getOrDefault("regNo", "2024CS101");
        String endpointBase = flags.getOrDefault("endpoint", "https://devapigw.vidalhealthtpa.com/srm-quiz-task");
        String modeRaw = flags.getOrDefault("mode", "live");
        Path outDir = Path.of(flags.getOrDefault("outDir", "out"));

        RunMode mode = switch (modeRaw.toLowerCase(Locale.ROOT)) {
            case "live" -> RunMode.LIVE;
            case "replay" -> RunMode.REPLAY;
            default -> throw new IllegalArgumentException("Invalid --mode: " + modeRaw + " (use live|replay)");
        };

        return new AppConfig(regNo, endpointBase, mode, outDir, dryRun);
    }

    static void printUsageAndExit(int code) {
        System.out.println("Usage: java -jar <jar> [options]\n" +
                "\nOptions:\n" +
                "  --regNo <value>       Registration number (default: 2024CS101)\n" +
                "  --endpoint <url>      Base endpoint (default: devapigw.../srm-quiz-task)\n" +
                "  --mode live|replay    Fetch from API or replay saved poll JSON (default: live)\n" +
                "  --outDir <path>       Output folder for audit + exports (default: out)\n" +
                "  --dryRun              Compute + export but do not submit\n" +
                "  --help                Print this help\n" +
                "\nExamples:\n" +
                "  mvn -q -DskipTests package && java -cp target/classes com.vidal.quiz.QuizApp --regNo 2024CS101\n" +
                "  java -cp target/classes com.vidal.quiz.QuizApp --mode replay --outDir out\n");
        System.exit(code);
    }
}

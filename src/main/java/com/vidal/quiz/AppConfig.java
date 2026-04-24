package com.vidal.quiz;

import java.nio.file.Path;

record AppConfig(
        String regNo,
        String endpointBase,
        RunMode mode,
        Path outDir,
        boolean dryRun
) {
    AppConfig {
        if (regNo == null || regNo.isBlank()) {
            throw new IllegalArgumentException("regNo is required");
        }
        if (endpointBase == null || endpointBase.isBlank()) {
            throw new IllegalArgumentException("endpointBase is required");
        }

        if (endpointBase.endsWith("/")) {
            endpointBase = endpointBase.substring(0, endpointBase.length() - 1);
        }

        if (mode == null) {
            throw new IllegalArgumentException("mode is required");
        }
        if (outDir == null) {
            throw new IllegalArgumentException("outDir is required");
        }
    }
}

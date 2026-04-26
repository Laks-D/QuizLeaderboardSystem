# Quiz Leaderboard System (Java 17)

This repository contains a Java 17 CLI that:

- Polls the quiz API **exactly 10 times** with a **5 second** interval (live mode)
- Deduplicates events by `roundId + participant`
- Builds a leaderboard sorted by `totalScore` (descending)
- Submits the final leaderboard (unless `--dryRun` is used)

Deduplication key: `roundId + "_" + participant` (idempotent across polls).

It also writes an audit trail and analytics-friendly exports so results are reproducible and easy to review.

## Prerequisites

- JDK 17
- Maven 3.9+

## Quick start (PowerShell)

### 1) Run tests

If `mvn` is available on PATH:

```powershell
mvn -q clean test
```

If `mvn` is not available on PATH, either add Maven to PATH or run it using the full path (example below for Windows):

```powershell
& "D:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd" -q clean test
```

### 2) Build + run (dry run)

Dry run computes everything but does not submit.

```powershell
mvn -q exec:java "-Dexec.args=--regNo <REG_NO> --dryRun --outDir out"
```

### 3) Run (live + submit)

```powershell
mvn -q exec:java "-Dexec.args=--regNo <REG_NO> --outDir out"
```

### 4) Replay mode (no network)

Replay mode uses files saved from a previous live run.

```powershell
mvn -q exec:java "-Dexec.args=--mode replay --outDir out --dryRun"
```

## API response (submission)

The submit endpoint typically responds with JSON similar to:

```json
{
	"regNo": "RA2311003020118",
	"totalPollsMade": 30,
	"submittedTotal": 1365,
	"attemptCount": 3
}
```

`submittedTotal` should match the printed `Total Score` from the program.
*This is an example output after testing. ( 3 attempts )

## Outputs

All outputs are written under `out/` by default:

- `out/raw/poll-<n>.json` — raw API response for each poll
- `out/leaderboard.json` — submission-shaped JSON (`regNo` + `leaderboard`)
- `out/leaderboard.csv` — CSV for Excel / Power BI
- `out/summary.json` — run summary (events processed, duplicates skipped, duration)

## CLI options

```text
--regNo <value>       Registration number (recommended)
--endpoint <url>      Base endpoint (default: https://devapigw.vidalhealthtpa.com/srm-quiz-task)
--mode live|replay    Fetch from API or replay saved poll JSON (default: live)
--outDir <path>       Output folder for audit + exports (default: out)
--dryRun              Compute + export but do not submit
--help                Print help
```

## Notes

- The program prints a "Total Score" line for quick verification.
- Live mode requires network access to the API endpoint.

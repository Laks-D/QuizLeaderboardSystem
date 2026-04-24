# SRM Quiz Distributor (Java 17)

This repository contains a Java 17 CLI that:

- Polls the quiz API **exactly 10 times** with a **5 second** interval (live mode)
- Deduplicates events by `roundId + participant`
- Builds a leaderboard sorted by `totalScore` (descending)
- Submits the final leaderboard (unless `--dryRun` is used)

It also writes an audit trail and analytics-friendly exports so results are reproducible and easy to review.

## What to change before submission

Use your real registration number via `--regNo`.

## Quick start (PowerShell)

### 1) Build + run (dry run)

Dry run computes everything but does not submit.

```powershell
mvn -q clean test
mvn -q exec:java -Dexec.args="--regNo 2024CS101 --dryRun"
```

### 2) Run (live + submit)

```powershell
mvn -q exec:java -Dexec.args="--regNo 2024CS101"
```

### 3) Replay mode (no network)

Replay mode uses files saved from a previous live run.

```powershell
mvn -q exec:java -Dexec.args="--mode replay --outDir out --dryRun"
```

## Outputs

All outputs are written under `out/` by default:

- `out/raw/poll-<n>.json` — raw API response for each poll
- `out/leaderboard.json` — submission-shaped JSON (`regNo` + `leaderboard`)
- `out/leaderboard.csv` — CSV for Excel / Power BI
- `out/summary.json` — run summary (events processed, duplicates skipped, duration)

## CLI options

```text
--regNo <value>       Registration number (default: 2024CS101)
--endpoint <url>      Base endpoint (default: https://devapigw.vidalhealthtpa.com/srm-quiz-task)
--mode live|replay    Fetch from API or replay saved poll JSON (default: live)
--outDir <path>       Output folder for audit + exports (default: out)
--dryRun              Compute + export but do not submit
--help                Print help
```

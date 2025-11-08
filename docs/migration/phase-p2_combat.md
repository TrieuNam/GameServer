# Phase P2 — Domain: Chiến đấu (Combat)

Mục tiêu
- Port battle-related modules: orchestrator, skill evaluation, buff handling, dungeon (instance) logic, matchmaking and combat logs.
- Preserve deterministic combat logic where required (reproduceable RNG or server-authoritative simulation).

Service candidates
- `battle-service` (8310) — orchestrator for PVE / PVP, fight loops
- `skill-service` (8320) — skill definitions, condition checks
- `buff-service` (8330) — buff/debuff lifecycle and stacking rules
- `dungeon-service` (8340) — instance creation, waves, AI scripts
- `matchmaking-service` (8350) — Elo/MMR queue and pairing
- `combat-log-service` (8360) — store detailed combat logs, anomaly detection
- `arena-service` (8370) — PvP seasons and results

Key technical considerations
- Performance: combat code is latency-sensitive. Use Netty, tuned thread pools, and consider native optimizations only where necessary.
- Determinism: keep RNG seeds and order of operations identical if you plan to reproduce fights or run replays.
- State ownership: prefer ephemeral in-memory simulation per match and commit only final results to DB (reduce chatty DB writes during fight).

Migration steps
1. Inventory battle logic in C++: find `battle_def`, `battle_script_def`, `battle_event_def`, `battle_buff_def` headers and map to Java data models (proto or DTOs).
2. Create `battle-service` skeleton using Netty for high-throughput internal messaging or plain Spring Boot with fixed worker pools if acceptable.
3. Implement skill simulation engine as a library (`battle-engine`) that can be shared between `battle-service` and tests. Translate algorithms from C++ carefully and add unit tests to match known outcomes (golden tests).
4. Implement `matchmaking-service` with job queue (Redis sorted set or Kafka topic) and ensure fairness and timeout handling.
5. Add `combat-log-service` to persist logs (consider Kafka topic for raw logs and a consumer that persists into `combatlog_db`).

Verification
- Create deterministic unit tests: run a recorded battle scenario in C++ (if available test vectors) and compare Java engine outputs.
- Load tests to validate throughput and latency under expected concurrency.

Risks
- Exact port of complex C++ combat logic is error-prone. Create many small unit tests and at least one replay test per major combat flow.
- JVM GC and CPU profile differences compared to native C++: may require tuning, or keep performance-critical loops in a self-contained native module if absolutely necessary.

Artifacts
- `services/battle-service/` with engine library `libs/battle-engine/` and test harness.
- Performance test scripts and benchmark results.

Next
- I can produce a `battle-engine` translation checklist: top C++ files to port (list) and a first unit test that verifies a simple fight outcome. Approve to proceed.
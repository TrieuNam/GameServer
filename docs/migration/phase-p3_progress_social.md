# Phase P3 — Tiến trình & Xã hội (Progress & Social)

Mục tiêu
- Port social and progression services: roles/characters, tasks/achievements, guilds, friends, mail, chat, leaderboards, activities.

Service mapping
- `role-service` (8410) — character model, attributes, level/exp
- `task-service` (8420) — tasks + achievements orchestration
- `guild-service` (8440) — guild membership and management
- `friend-service` (8450) — friends/block lists
- `mail-service` (8460) — system/player mail
- `chat-service` (8470) — real-time chat (WS channels/rooms)
- `leaderboard-service` (8480) — snapshots, season management
- `activity-service` (8490) — time-limited events

Concrete steps
1. Role-service first (owner of role state)
   - Implement role CRUD and attribute update APIs.
   - Use optimistic locking/versioned updates to avoid lost updates.
   - Provide snapshot endpoints for leaderboards.
2. Chat-service
   - Use `webSocket-server` infra for real-time messages; chat-service stores history and moderates.
3. Leaderboard
   - Use Redis sorted sets for live leaderboard; persist snapshots to `lb_db` periodically.
4. Activities and task-service
   - Implement state machines for tasks; ensure idempotence and event-sourced updates when possible.

Verification
- Integration tests: role create -> complete task -> update leaderboard -> confirm snapshot.
- Chat load test: simulate many WS clients joining channels.

Risks
- Social features often require complex consistency for membership/roles; define clear owner services and invariants.
- Chat scaling: may need partitioning by region or channel.

Artifacts
- `services/role-service` skeleton, `services/chat-service` with basic WS controllers, `services/leaderboard-service` with Redis usage.

Next
- I can scaffold `role-service` (skeleton CRUD + Flyway) and `chat-service` (WS sample) now if you want.
# Service Doctor Copilot Prompt

Service: leaderboard-service
Display name: Leaderboard Service
Phase: P3
Port: 8480
Current service status: STOPPED
Current doctor status: FIXING
Detected error type: -
Summary: Service is running normally

## Recent logs
```text
2026-04-05T20:46:19.006+07:00  INFO 61940 --- [leaderboard-service] [   scheduling-1] c.S.l.service.LeaderboardService         : Cache cleared for ranking type: 3
Hibernate: select re1_0.id,re1_0.current_rank,re1_0.guild_name,re1_0.previous_rank,re1_0.ranking_type,re1_0.role_id,re1_0.role_level,re1_0.role_name,re1_0.score,re1_0.updated_at from ranking_entry re1_0 where re1_0.ranking_type=? order by re1_0.score desc,re1_0.updated_at limit ?
2026-04-05T20:46:19.010+07:00  INFO 61940 --- [leaderboard-service] [   scheduling-1] c.S.l.service.LeaderboardService         : Cache cleared for ranking type: 4
Hibernate: select re1_0.id,re1_0.current_rank,re1_0.guild_name,re1_0.previous_rank,re1_0.ranking_type,re1_0.role_id,re1_0.role_level,re1_0.role_name,re1_0.score,re1_0.updated_at from ranking_entry re1_0 where re1_0.ranking_type=? order by re1_0.score desc,re1_0.updated_at limit ?
2026-04-05T20:46:19.014+07:00  INFO 61940 --- [leaderboard-service] [   scheduling-1] c.S.l.service.LeaderboardService         : Cache cleared for ranking type: 5
Hibernate: select re1_0.id,re1_0.current_rank,re1_0.guild_name,re1_0.previous_rank,re1_0.ranking_type,re1_0.role_id,re1_0.role_level,re1_0.role_name,re1_0.score,re1_0.updated_at from ranking_entry re1_0 where re1_0.ranking_type=? order by re1_0.score desc,re1_0.updated_at limit ?
2026-04-05T20:46:19.018+07:00  INFO 61940 --- [leaderboard-service] [   scheduling-1] c.S.l.service.LeaderboardService         : Cache cleared for ranking type: 6
Hibernate: select re1_0.id,re1_0.current_rank,re1_0.guild_name,re1_0.previous_rank,re1_0.ranking_type,re1_0.role_id,re1_0.role_level,re1_0.role_name,re1_0.score,re1_0.updated_at from ranking_entry re1_0 where re1_0.ranking_type=? order by re1_0.score desc,re1_0.updated_at limit ?
2026-04-05T20:46:19.022+07:00  INFO 61940 --- [leaderboard-service] [   scheduling-1] c.S.l.service.LeaderboardService         : Cache cleared for ranking type: 7
Hibernate: select re1_0.id,re1_0.current_rank,re1_0.guild_name,re1_0.previous_rank,re1_0.ranking_type,re1_0.role_id,re1_0.role_level,re1_0.role_name,re1_0.score,re1_0.updated_at from ranking_entry re1_0 where re1_0.ranking_type=? order by re1_0.score desc,re1_0.updated_at limit ?
2026-04-05T20:46:19.026+07:00  INFO 61940 --- [leaderboard-service] [   scheduling-1] c.S.l.service.LeaderboardService         : Cache cleared for ranking type: 8
2026-04-05T20:46:19.026+07:00  INFO 61940 --- [leaderboard-service] [   scheduling-1] c.S.l.service.LeaderboardService         : All leaderboards refreshed!
```

## Task
- find the root cause
- propose the smallest safe fix
- do not change unrelated files
- keep current behavior intact
- ensure the relevant Maven build passes after the fix

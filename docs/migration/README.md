# Migration plan: C++ -> Java (Spring)

This folder contains step-by-step migration guidance split by Phases (P0..P4) plus Preparation.
Each Phase file explains goals, mapping from C++ modules to Java microservices, concrete tasks, verification steps, risks and artifacts.

Files:
- `phase-0_preparation.md`  — preparation tasks: proto, schema, config extraction
- `phase-p0_infra.md`      — platform & infrastructure (gateway, config, discovery, kafka, redis ...)
- `phase-p1_economy.md`    — domain: wallet, item, bag, equip, shop, gift, crafting, box
- `phase-p2_combat.md`     — combat & battle domain: battle, skill, buff, dungeon, matchmaking, arena
- `phase-p3_progress_social.md` — roles, tasks, guild, mail, chat, leaderboard, activity
- `phase-p4_other.md`      — analytics, notification, report, file, scheduler, localization, anti-cheat

How to use
1. Read `phase-0_preparation.md` and run the extraction steps to produce `common/proto` and SQL artifacts.
2. Boot P0 infra services and add the `proto` module to Java build system.
3. Follow each Phase file to pick and port one service at a time.

If you want I can now generate the Java `proto` module skeleton and/or create a first service skeleton (e.g., `user-service` or `bag-service`).

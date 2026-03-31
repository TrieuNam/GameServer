# GameServer Skill Agent - Setup Complete ✅

## Summary

Skill `gameserver-skill-memory` da duoc setup va san sang su dung.

## Whats Installed

### 1. Skill Locations
```
D:\project\serverGame\GameServer\
├── .github/skills/gameserver-skill-memory/     ← GitHub Copilot tu day load
├── .claude/skills/gameserver-skill-memory/     ← Claude Code tu day load
└── docs/skill_agent/ai-agents-skills/skills/gameserver-skill-memory/  ← Master copy
```

### 2. Skill Content
```
gameserver-skill-memory/
├── SKILL.md                                 (Main runbook - 4 playbooks)
├── service-memories/
│   └── TASK_SERVICE_MEMORY.md              (Task service chi tiet)
└── references/
    ├── COMMAND_SNIPPETS.md                 (Build/test commands)
    ├── CONFIG_QUICK_REFERENCE.md           (YAML, env, schemas)
    └── SERVICE_INTERACTION_PATTERNS.md     (Feign, events, saga)
```

### 3. Guide Files (Root)
```
D:\project\serverGame\GameServer\
├── HOW_TO_USE_SKILLS.md        (Trang huong dan chi tiet)
└── SKILL_QUICK_START.md        (Trang khoi dong nhanh)
```

---

## How to Use NOW

### Method 1: GitHub Copilot (Recommended)

1. Open folder: `D:\project\serverGame\GameServer` in VS Code
2. Open Chat: `Ctrl+Shift+I`
3. Type prompt:
   ```
   @gameserver Fix task-service bug when creating task
   ```

### Method 2: Claude Code

1. Chat voi Claude, mention skill:
   ```
   Use gameserver-skill-memory skill.
   Fix bug trong task-service...
   ```

### Method 3: Direct Question

```
@gameserver Show TASK_SERVICE_MEMORY. Co API nao de delete task?
@gameserver Show COMMAND_SNIPPETS. Lam sao test TaskDomainServiceTest?
@gameserver What is Playbook A? (Bug Fix)
```

---

## Trigger Keywords

Use any of these:
- `@gameserver` (direct)
- `gameserver context`, `skill memory`
- `task-service` (service-specific)
- `bug fix`, `feature`, `refactor`, `review`
- `playbook`, `risk gate`, `checklist`

---

## Playbooks Available

| Playbook | When to Use |
|----------|-------------|
| **A - Bug Fix** | Loi co san, can fix nhanh |
| **B - Small Feature** | Add feature nho moi |
| **C - Refactor** | Sua cau truc, khong doi hanh vi |
| **D - Code Review** | Review code tim issue |

---

## Example Prompts

### Bug Fix
```
@gameserver 
Bug: Task status khong update khi event bus fail trong task-service.
File: TaskService.java line 45
Dung Playbook A.
```

### Feature
```
@gameserver
Can them task priority sorting trong task-service.
Dung Playbook B.
```

### Review
```
@gameserver
Review TaskService.java.
Dung Playbook D va Risk Gate.
```

### Learn
```
@gameserver
Show TASK_SERVICE_MEMORY.
1. Database schema
2. API endpoints
3. Common bugs
```

---

## Structure on Disk

```
GameServer/
├── .github/skills/gameserver-skill-memory/
│   ├── SKILL.md
│   ├── service-memories/TASK_SERVICE_MEMORY.md
│   └── references/
│       ├── COMMAND_SNIPPETS.md
│       ├── CONFIG_QUICK_REFERENCE.md
│       └── SERVICE_INTERACTION_PATTERNS.md
│
├── .claude/skills/gameserver-skill-memory/  (same as above)
│
├── docs/skill_agent/ai-agents-skills/skills/gameserver-skill-memory/  (master)
│
├── HOW_TO_USE_SKILLS.md       (detailed guide)
├── SKILL_QUICK_START.md        (quick reference)
└── README.md                    (project readme)
```

---

## Files to Know

| File | Purpose | Read When |
|------|---------|-----------|
| `SKILL.md` | Main skill, playbooks | Always first |
| `TASK_SERVICE_MEMORY.md` | Task service specifics | Working with task-service |
| `COMMAND_SNIPPETS.md` | Build/test commands | Need to run commands |
| `CONFIG_QUICK_REFERENCE.md` | Configs, schemas | Setting up services |
| `SERVICE_INTERACTION_PATTERNS.md` | How services talk | Debugging cross-service |
| `HOW_TO_USE_SKILLS.md` | Full guide | Setup, troubleshooting |
| `SKILL_QUICK_START.md` | Quick examples | First time using |

---

## Important: Keep in Sync

Master copy is in: `docs/skill_agent/ai-agents-skills/skills/gameserver-skill-memory/`

When you update skill there, copy to:
- `.github/skills/gameserver-skill-memory/`
- `.claude/skills/gameserver-skill-memory/`

Or run sync script:
```powershell
# In PowerShell
$src = "docs\skill_agent\ai-agents-skills\skills\gameserver-skill-memory"
Copy-Item -Path $src -Destination ".github\skills\gameserver-skill-memory" -Recurse -Force
Copy-Item -Path $src -Destination ".claude\skills\gameserver-skill-memory" -Recurse -Force
```

---

## Testing Skill

Test voi prompt nay:
```
@gameserver What playbooks are available?
```

Expected: Agent lists Playbook A, B, C, D

If not working:
- [ ] Verify `.github/skills/gameserver-skill-memory/SKILL.md` exists
- [ ] Check SKILL.md not empty
- [ ] Restart VS Code
- [ ] Clear Copilot cache (settings)

---

## Whats Next

1. ✅ Skill installed at `.github/skills/` va `.claude/skills/`
2. ✅ TASK_SERVICE_MEMORY created (reference for task-service work)
3. ✅ Reference guides created (commands, configs, patterns)
4. ✅ Quick start guides created

Ready to use:
- Open VS Code
- Type `@gameserver ...` in chat
- Agent loads skill + memories + playbooks
- Follow agent guidance + checklists
- Bao cao theo communication template

---

## Last Important Note

⚠️ **When you edit skill:**
1. Edit MASTER copy in `docs/skill_agent/ai-agents-skills/skills/gameserver-skill-memory/SKILL.md`
2. Copy to `.github/skills/gameserver-skill-memory/` 
3. Copy to `.claude/skills/gameserver-skill-memory/`
4. Restart IDE
5. Update log in SKILL.md footer

---

## Support

Read these files:
- `SKILL_QUICK_START.md` — Fast start
- `HOW_TO_USE_SKILLS.md` — Full guide
- `.github/skills/gameserver-skill-memory/SKILL.md` — Skill details

Questions? Look in references folder or skill memory.

---

**Setup Complete. Ready to use skill! 🚀**


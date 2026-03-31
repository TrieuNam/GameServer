# QUICK START - Su Dung Skill Ngay

## 3 Buoc De Activate Skill

### Step 1: Open GameServer Folder
```powershell
Set-Location "D:\project\serverGame\GameServer"
# Then open in VS Code hoac IDE
code .
```

### Step 2: Open GitHub Copilot Chat
- Nhan `Ctrl+Shift+I` (VS Code)
- Hoac click Chat icon ben trai

### Step 3: Gõ Prompt voi Trigger Keyword

**Ví dụ 1 - Bug Fix:**
```
@gameserver Fix bug trong task-service: task status khong update khi event bus fail
```

**Ví dụ 2 - Feature:**
```
@gameserver Them feature permission check trong task-service create task
```

**Ví dụ 3 - Review:**
```
@gameserver Review file task-service/src/main/java/.../TaskService.java. Dung Playbook D.
```

**Ví dụ 4 - Access Memory:**
```
@gameserver Show TASK_SERVICE_MEMORY. API nao de get user tasks?
```

**Ví dụ 5 - Get Commands:**
```
@gameserver Show COMMAND_SNIPPETS. Lam sao build va test task-service?
```

---

## Trigger Keywords

Agent se activate skill khi nhan dien:

| Keyword | What It Triggers |
|---------|------------------|
| `@gameserver` | Load skill directly |
| `gameserver context` | Load context |
| `task-service` | Service-specific memory |
| `bug fix` | Playbook A |
| `feature` | Playbook B |
| `refactor` | Playbook C |
| `review checklist` | Playbook D |
| `risk gate` | Risk gate checklist |
| `task playbook` | Show all playbooks |

---

## Skill Structure (Tìm gì?)

```
Skill Content:
├── SKILL.md (Main runbook)
│   ├── Activation Rules
│   ├── Non-Negotiable Rules
│   ├── Task Playbooks A-D
│   └── Risk Gate
├── service-memories/
│   └── TASK_SERVICE_MEMORY.md (APIs, DB, tests, bugs)
└── references/
    ├── COMMAND_SNIPPETS.md (build/test commands)
    ├── CONFIG_QUICK_REFERENCE.md (yml, env, schemas)
    └── SERVICE_INTERACTION_PATTERNS.md (Feign, events, saga)
```

---

## Common Prompts

### I. Bug Fix Flow
```
@gameserver 
Loi: Task status khong update khi save.
File: task-service/src/main/java/.../TaskService.java (line 45)
Dung Playbook A - Bug Fix.
```

### II. Feature Add
```
@gameserver
Can them: Task priority sorting trong list.
File: TaskService.java, TaskRepository.java
Dung Playbook B - Small Feature.
```

### III. Code Review
```
@gameserver
Review file: task-service/src/main/java/.../TaskController.java
Dung Playbook D + Risk Gate.
```

### IV. Understanding Service
```
@gameserver
Show TASK_SERVICE_MEMORY.
1. Co API nao de delete task?
2. Luat nao trong Task status transition?
3. Co cross-service call nao?
```

### V. Quick Commands
```
@gameserver
Show COMMAND_SNIPPETS.
Lam sao chay test class TaskDomainServiceTest?
```

---

## Expected Agent Response

Khi skill hoat dong, agent se:

1. ✅ Load SKILL.md va recognize playbook
2. ✅ Suggest risk checklist items
3. ✅ Reference TASK_SERVICE_MEMORY o dung vi tri
4. ✅ Provide command snippets tu references
5. ✅ Remind "chua verify" neu chua chay test
6. ✅ Follow communication template khi bao cao

---

## Verify Skill is Working

Test voi prompt nay:
```
@gameserver What playbooks do I have for task-service?
```

Expected response:
- Playbook A - Bug Fix
- Playbook B - Small Feature
- Playbook C - Refactor
- Playbook D - Code Review

Neu khong thay, check:
- [ ] Folder ton tai: `.github/skills/gameserver-skill-memory/SKILL.md`
- [ ] SKILL.md co content (khong rong)
- [ ] Restart IDE
- [ ] Clear Copilot cache

---

## File Locations (Reference)

| File | Location |
|------|----------|
| Main Skill | `.github/skills/gameserver-skill-memory/SKILL.md` |
| Task Memory | `.github/skills/gameserver-skill-memory/service-memories/TASK_SERVICE_MEMORY.md` |
| Commands | `.github/skills/gameserver-skill-memory/references/COMMAND_SNIPPETS.md` |
| Config | `.github/skills/gameserver-skill-memory/references/CONFIG_QUICK_REFERENCE.md` |
| Patterns | `.github/skills/gameserver-skill-memory/references/SERVICE_INTERACTION_PATTERNS.md` |
| Master Copy | `docs/skill_agent/ai-agents-skills/skills/gameserver-skill-memory/` |

---

## Notes

⚠️ **Update Skill:**
Khi cap nhat, edit master copy trong `docs/skill_agent/ai-agents-skills/...` 
Roi copy lai vao `.github/skills/` va `.claude/skills/`

⚠️ **Restart IDE Sau Copy:**
Close va reopen VS Code de agent reload skill.

---

## Next Steps

1. **Test now**: Open VS Code, type `@gameserver What is Playbook A?`
2. **Read memory**: Ask `@gameserver Show TASK_SERVICE_MEMORY`
3. **Use commands**: Ask `@gameserver How to test task-service?`
4. **Create service memory** cho service khac khi can


# Cach Su Dung GameServer Skill

Skill `gameserver-skill-memory` da duoc copy vao dung vi tri de agent tu dong detect.

## Structure

```
GameServer/
├── .github/skills/gameserver-skill-memory/      (GitHub Copilot)
├── .claude/skills/gameserver-skill-memory/      (Claude Code)
└── docs/skill_agent/ai-agents-skills/skills/gameserver-skill-memory/  (Master copy)
```

## Kich Hoat Skill

### Option 1: GitHub Copilot (VS Code)

1. Mo project folder `D:\project\serverGame\GameServer` trong VS Code
2. Mo GitHub Copilot Chat panel (Ctrl+Shift+I)
3. Type prompt voi trigger keywords:
   ```
   @gameserver Fix bug trong task-service
   @gameserver Review code cho task-service
   @gameserver Them feature moi trong task-service
   ```
4. Copilot se tu dong load skill `gameserver-skill-memory` va follow playbooks

**Trigger keywords** (agent se nhan dien va load skill):
- `gameserver`, `gameserver context`, `skill memory`
- `review checklist`, `risk checklist`
- `bug fix`, `feature`, `refactor`, `task playbook`
- `task-service` (service-specific)

### Option 2: Claude Code (Claude Desktop / Web)

1. Tro chat voi Claude va mention skill:
   ```
   Dung skill gameserver-skill-memory. 
   Giup fix bug trong task-service ...
   ```

2. Hoac neu Claude integrate vao folder:
   - Open folder `D:\project\serverGame\GameServer`
   - Claude se scan `.claude/skills/` va load automatically

### Option 3: GitHub Copilot via Command Line

```powershell
# Neu dung Copilot CLI (experimental)
github-copilot chat "Fix task-service bug when creating task"
```

## Prompt Examples

### Bug Fix (Activate Playbook A)
```
@gameserver 
Fix bug trong task-service: task status khong update khi event bus fail. 
Dung Playbook A - Bug Fix.
```

### Feature Request (Activate Playbook B)
```
@gameserver
Them feature "kiem tra permission" trong task-service.
Dung Playbook B - Small Feature.
```

### Code Review (Activate Playbook D)
```
@gameserver
Review code file `task-service/src/main/java/...TaskService.java`.
Dung Playbook D - Code Review. Liet ke findings theo Critical > High > Low.
```

### Access Service Memory
```
@gameserver
Show TASK_SERVICE_MEMORY. Co API nao de get tasks cua user?
```

### Access References
```
@gameserver
Show COMMAND_SNIPPETS. Lam sao chay test cho task-service?
```

## Agent Configuration

### GitHub Copilot
File: `.github/copilot-instructions.md` (optional, for custom instructions)

```markdown
# Custom Instructions for GitHub Copilot

## GameServer Skills
- Luon check `.github/skills/gameserver-skill-memory/SKILL.md` khi lam viec voi GameServer
- Luon follow playbooks da dinh nghia
- Luon check risk gate truoc khi submit
```

### Claude Code
File: `.claude/CLAUDE.md` (optional)

```markdown
# Claude Configuration for GameServer

## Available Skills
- gameserver-skill-memory: Operational skill cho GameServer microservices

## When to Activate
- Bug fix, feature, refactor, review tasks
- Prompt keywords: gameserver, task-service, review checklist, risk gate
```

## Verify Skill is Loaded

### In GitHub Copilot Chat
1. Type `@gameserver` — Copilot should suggest skill completion
2. Type `/help gameserver` — should show skill info
3. Type `Show gameserver-skill-memory` — skill content should appear

### In Claude
1. Mention `gameserver-skill-memory` in chat
2. Ask `What playbooks are available?`
3. Claude should reference SKILL.md content

## Troubleshooting

### Skill Not Detected
- [ ] Verify folder exists: `D:\project\serverGame\GameServer\.github/skills/gameserver-skill-memory/`
- [ ] Check file `SKILL.md` co ton tai va co content
- [ ] Restart IDE (VS Code, Claude Desktop)
- [ ] Clear cache (usually in IDE settings)

### Skill Detected nhung khong load content
- [ ] Check SKILL.md frontmatter (name, description)
- [ ] Verify YAML syntax la dung
- [ ] Check permissions (file phai readable)

### Agent khong follow playbook
- [ ] Trong prompt, explicitly mention playbook: `Dung Playbook A - Bug Fix`
- [ ] Use trigger keywords: `review checklist`, `risk gate`, `task playbook`
- [ ] Reference file: `Xem TASK_SERVICE_MEMORY.md`

## Next Steps

### Step 1: Test Skill
```
Open GameServer folder in VS Code
Open GitHub Copilot Chat
Type: @gameserver Cau truc cua task-service nhu the nao?
```

### Step 2: Use Service Memory
```
@gameserver Show TASK_SERVICE_MEMORY. Co moi risk nao toi uu?
```

### Step 3: Follow Playbook
```
@gameserver 
Tim bug trong task-service TaskService.java.
Dung Playbook A - Bug Fix va Risk Gate.
```

### Step 4: Create More Service Memories (Optional)
Copy `service-memories/TASK_SERVICE_MEMORY.md` va tao moi cho:
- `USER_SERVICE_MEMORY.md`
- `GUILD_SERVICE_MEMORY.md`
- etc.

## References

- **Skill Manifest**: `D:\project\serverGame\GameServer\.github\skills\gameserver-skill-memory\SKILL.md`
- **Service Memory**: `D:\project\serverGame\GameServer\.github\skills\gameserver-skill-memory\service-memories\TASK_SERVICE_MEMORY.md`
- **Command Reference**: `D:\project\serverGame\GameServer\.github\skills\gameserver-skill-memory\references\COMMAND_SNIPPETS.md`
- **Master Copy**: `D:\project\serverGame\GameServer\docs\skill_agent\ai-agents-skills\skills\gameserver-skill-memory\`

## Important Notes

⚠️ **Khi cap nhat skill:**
1. Cap nhat trong `docs/skill_agent/ai-agents-skills/skills/gameserver-skill-memory/` (master copy)
2. Sau do copy lai vao `.github/skills/gameserver-skill-memory/` va `.claude/skills/gameserver-skill-memory/`
3. Hoac: tao script de tu dong sync khi update

⚠️ **Sync Reminder Script** (optional, PowerShell):
```powershell
# file: sync-skills.ps1
$source = "D:\project\serverGame\GameServer\docs\skill_agent\ai-agents-skills\skills\gameserver-skill-memory"
$targets = @(
  "D:\project\serverGame\GameServer\.github\skills\gameserver-skill-memory",
  "D:\project\serverGame\GameServer\.claude\skills\gameserver-skill-memory"
)

foreach ($target in $targets) {
  if (Test-Path $target) {
    Remove-Item $target -Recurse -Force
  }
  Copy-Item -Path $source -Destination $target -Recurse
  Write-Host "Synced to $target"
}
```

Run: `.\sync-skills.ps1` sau khi update skill.


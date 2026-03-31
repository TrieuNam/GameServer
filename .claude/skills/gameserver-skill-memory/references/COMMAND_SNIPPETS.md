# Command Snippets — GameServer

Cac lenh PowerShell dung de build, test, verify GameServer nhanh.

## Build Commands

### Build Toan Bo (with Tests)
```powershell
Set-Location "D:\project\serverGame\GameServer"
mvn -T 1C clean install
```

### Build Toan Bo (Skip Tests — Nhanh)
```powershell
Set-Location "D:\project\serverGame\GameServer"
mvn -T 1C clean install -DskipTests
```

### Build 1 Service
```powershell
Set-Location "D:\project\serverGame\GameServer\task-service"
mvn clean install

# Or skip tests
mvn clean install -DskipTests
```

## Test Commands

### Run All Tests cho 1 Service
```powershell
Set-Location "D:\project\serverGame\GameServer\task-service"
mvn test
```

### Run 1 Test Class
```powershell
Set-Location "D:\project\serverGame\GameServer\task-service"
mvn -Dtest=TaskDomainServiceTest test
```

### Run 1 Test Method
```powershell
Set-Location "D:\project\serverGame\GameServer\task-service"
mvn -Dtest=TaskDomainServiceTest#testCreateTask test
```

### Run Tests voi Coverage (if Maven Jacoco configured)
```powershell
Set-Location "D:\project\serverGame\GameServer\task-service"
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

## Verify Commands

### Check Compilation Errors (Dry Run)
```powershell
Set-Location "D:\project\serverGame\GameServer"
mvn clean compile
```

### Check for Dependency Issues
```powershell
Set-Location "D:\project\serverGame\GameServer"
mvn dependency:tree
# Or check specific service
mvn -pl task-service dependency:tree
```

### Analyze Code (Static Analysis)
```powershell
# If SonarQube is configured
mvn -Dsonar.projectKey=gameserver sonar:sonar
```

## Clean Commands

### Clean Single Service Build
```powershell
Set-Location "D:\project\serverGame\GameServer\task-service"
mvn clean
```

### Clean All Builds
```powershell
Set-Location "D:\project\serverGame\GameServer"
mvn clean
```

### Remove Maven Cache (Last Resort)
```powershell
Remove-Item -Recurse -Force -Path "$env:USERPROFILE\.m2\repository"
# Then rebuild (sẽ download lại tất cả dependencies, có thể lâu)
```

## Diagnostic Commands

### View Maven Properties
```powershell
Set-Location "D:\project\serverGame\GameServer"
mvn help:active-profiles
mvn help:describe -Dcmd=test
```

### Check Java Version (Must Be 11+)
```powershell
java -version
```

### Check Maven Version
```powershell
mvn -version
```

## Common Issues & Fixes

### Issue: Build Fails with "Class Not Found"
```powershell
# Solution 1: Clean and rebuild
mvn clean install

# Solution 2: Clear Maven cache and rebuild
Remove-Item -Recurse -Force -Path "$env:USERPROFILE\.m2\repository"
mvn clean install
```

### Issue: Test Timeout
```powershell
# Increase timeout
mvn -DargLine="-Dcom.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.PACKET_SIZE=32768" test

# Or skip slow tests
mvn test -DskipSlowTests=true
```

### Issue: Out of Memory (OOM)
```powershell
# Increase Maven heap size
$env:MAVEN_OPTS = "-Xmx2G -Xms1G"
mvn clean install
```

## Service-Specific Snippets

### Task Service
```powershell
Set-Location "D:\project\serverGame\GameServer\task-service"
mvn -Dtest=TaskDomainServiceTest test
```

### User Service
```powershell
Set-Location "D:\project\serverGame\GameServer\user-service"
mvn test
```

### Gateway Service
```powershell
Set-Location "D:\project\serverGame\GameServer\gateway-service"
mvn clean install -DskipTests
```

## Database Commands

### Connect to Task Service DB
```powershell
# MySQL command (if installed)
mysql -h localhost -P 3306 -u root -p task_service_db
```

### Quick SQL Checks
```sql
-- Check table structure
DESCRIBE tasks;

-- Count records
SELECT COUNT(*) FROM tasks;

-- View recent records
SELECT * FROM tasks ORDER BY created_at DESC LIMIT 10;
```

## Troubleshooting Checklist
- [ ] Java version >= 11
- [ ] Maven version >= 3.6
- [ ] Internet connection (for downloading dependencies)
- [ ] Port 9015+ available (for services to start)
- [ ] Database running (if local MySQL required)
- [ ] No other mvn process running (use `taskkill /IM java.exe` if needed)

## Client-Server Mapping (MsgId/Proto)

### Locate MsgId from Client Source
```powershell
Set-Location "D:/project/serverGame"
Select-String -Path "document/client/LineR/assets/script/manager/MsgIdManger.ts" -Pattern "1470|1471|1480|1481"
```

### Verify MsgId on WebSocket Server
```powershell
Set-Location "D:/project/serverGame/GameServer"
Select-String -Path "webSocket-server/src/main/java/com/SouthMillion/webSocket_server/net/MsgIds.java" -Pattern "1470|1471|1480|1481"
```

### Verify Skill Handler Emit/Parse Path
```powershell
Set-Location "D:/project/serverGame/GameServer"
Select-String -Path "webSocket-server/src/main/java/com/SouthMillion/webSocket_server/handler/skill/SkillHandler.java" -Pattern "PB_CSRoleSkillOperaReq|PB_CSRoleTalentOperaReq|Emitters.emit"
```

### Verify Proto Anchor on Server
```powershell
Set-Location "D:/project/serverGame/GameServer"
Select-String -Path "common-lib/src/main/proto/sc/msgskill.proto" -Pattern "1470|1471|1480|1481|PB_CSRoleSkillOperaReq|PB_SCRoleTalentAllInfo"
```

### Verify Client Skill Config Anchors
```powershell
Set-Location "D:/project/serverGame"
Get-ChildItem "document/client/LineR/assets/resources/config" -Filter "*skill*.json"
```


# 🚀 Maven Build Guide - Game Server

## 📋 Quick Start

### Option 1: Interactive Menu (Recommended)

**Windows CMD:**
```cmd
build.cmd
```

**Windows PowerShell:**
```powershell
.\build.ps1
```

**Linux/Mac:**
```bash
chmod +x build.sh
./build.sh
```

### Option 2: Direct Commands

```bash
# Clean all
mvn clean

# Compile all (skip tests)
mvn compile -DskipTests

# Install all (skip tests)
mvn install -DskipTests

# Build P0 only (fastest)
mvn clean install -P p0,fast

# Build P0 + P1
mvn clean install -P p0-p1,fast

# Full build with tests
mvn clean install
```

---

## 📊 Build Profiles

### 1. Default (All Modules)
```bash
mvn clean install -DskipTests
```
**Builds:** All 24+ modules  
**Time:** ~5-10 minutes  
**Use:** Full deployment

### 2. P0 Profile (Core Services)
```bash
mvn clean install -P p0,fast
```
**Builds:** 
- common-lib
- eureka-server
- gateway-service
- config-service
- session-service
- webSocket-server
- user-service
- role-service
- report-service

**Time:** ~2-3 minutes  
**Use:** Basic game functionality

### 3. P0-P1 Profile (Core + Domain)
```bash
mvn clean install -P p0-p1,fast
```
**Builds:** P0 + P1 services  
**Modules:** 18 services  
**Time:** ~3-5 minutes  
**Use:** Full game features

### 4. Fast Profile
```bash
mvn clean install -P fast
```
**Effect:** Skip all tests  
**Use:** Quick iteration during development

---

## 🎯 Common Use Cases

### Quick Development Build
```bash
# Fastest - P0 only, no tests
mvn clean install -P p0,fast

# Or use script
build.cmd
# Select: 7. Clean + Install P0
```

### Production Build
```bash
# All modules with tests
mvn clean install

# Or use script
build.cmd
# Select: 9. Full Build (with tests)
```

### Update common-lib
```bash
# Build common-lib first
cd common-lib
mvn clean install -DskipTests

# Then build all services
cd ..
mvn install -DskipTests
```

### Rebuild Single Service
```bash
# Example: rebuild bag-service
cd bag-service
mvn clean install -DskipTests
```

---

## 📦 Module Structure

```
GameServer/
├── pom.xml                    ← Parent POM
├── build.cmd                  ← Windows build script
├── build.ps1                  ← PowerShell build script
│
├── common-lib/                ← Shared libraries (DTOs, Utils)
│
├── Infrastructure (P0)
│   ├── eureka-server/         ← Service discovery
│   ├── gateway-service/       ← API Gateway
│   ├── config-service/        ← Config management
│   ├── session-service/       ← Authentication
│   └── webSocket-server/      ← WebSocket handler
│
├── Core Services (P0)
│   ├── user-service/          ← User management
│   ├── role-service/          ← Character management
│   └── report-service/        ← Reporting
│
└── Domain Services (P1)
    ├── item-service/          ← Item metadata
    ├── bag-service/           ← Inventory
    ├── equip-service/         ← Equipment
    ├── wallet-service/        ← Currency
    ├── box-service/           ← Box/chest
    ├── drop-service/          ← Drop system
    ├── shop-service/          ← Shop
    ├── gift-service/          ← Gifts/rewards
    └── crafting-service/      ← Crafting
```

---

## ⚡ Performance Tips

### 1. Parallel Build
```bash
# Use multiple threads
mvn clean install -T 4 -DskipTests
# -T 4 = 4 threads
```

### 2. Offline Mode (if dependencies cached)
```bash
mvn clean install -o -DskipTests
# -o = offline mode
```

### 3. Skip JavaDoc
```bash
mvn clean install -Dmaven.javadoc.skip=true -DskipTests
```

### 4. Resume from failure
```bash
mvn install -rf :failed-module-name -DskipTests
# -rf = resume from
```

---

## 🔧 Troubleshooting

### Issue: "Project not found"
**Solution:** Run from GameServer root directory
```bash
cd D:\project\serverGame\GameServer
mvn clean install
```

### Issue: common-lib not found
**Solution:** Build common-lib first
```bash
cd common-lib
mvn clean install
cd ..
mvn install
```

### Issue: Out of memory
**Solution:** Increase Maven heap
```bash
set MAVEN_OPTS=-Xmx2048m -XX:MaxPermSize=512m
mvn clean install
```

### Issue: Port conflicts during tests
**Solution:** Skip tests
```bash
mvn clean install -DskipTests
```

---

## 📋 Build Order

Maven automatically determines build order based on dependencies:

1. **common-lib** (no dependencies)
2. **eureka-server** (depends on common-lib)
3. **config-service** (depends on common-lib)
4. **session-service** (depends on common-lib)
5. **All other services** (depend on common-lib)

---

## 🎨 PowerShell Usage Examples

### Interactive Menu
```powershell
.\build.ps1
```

### Direct Command
```powershell
# Build P0 only
.\build.ps1 -Action p0

# Clean + Install all
.\build.ps1 -Action all

# Compile only
.\build.ps1 -Action compile
```

---

## ✅ Verification

After build, check:

```bash
# Verify all JARs built
dir /s /b target\*.jar

# Or PowerShell
Get-ChildItem -Recurse -Filter *.jar | Select-Object FullName
```

Expected output:
- common-lib-1.0.0.jar
- eureka-server-1.0.0.jar
- gateway-service-1.0.0.jar
- ... (all services)

---

## 🚀 Quick Reference

| Command | Description | Time |
|---------|-------------|------|
| `mvn clean` | Clean all | ~30s |
| `mvn compile -DskipTests` | Compile all | ~2min |
| `mvn install -DskipTests` | Install all | ~3min |
| `mvn clean install -P p0,fast` | Build P0 | ~2min |
| `mvn clean install -P p0-p1,fast` | Build P0+P1 | ~3min |
| `mvn clean install -DskipTests` | Build all (no tests) | ~5min |
| `mvn clean install` | Build all (with tests) | ~10min |

---

## 📝 Notes

- **Always build common-lib first** if you made changes
- **Use profiles** to speed up development builds
- **Skip tests** during development, run before commit
- **Use parallel build** (`-T`) for faster builds
- **Check logs** in `target/` folders for errors

---

**Created:** 2025-11-15  
**For:** Game Server Development Team  
**Maven Version:** 3.8+  
**Java Version:** 21



# Wallet Service Memory

Service-specific operational memory cho `wallet-service` trong GameServer.

## Identity
- Service name: `wallet-service`
- Path: `GameServer/wallet-service`
- Main port: 9021
- Database: wallet_db (MySQL)
- Build: Maven (`mvn clean install`)

## Core Scope
- User wallet management (balance, currency)
- Transaction history tracking
- Payment processing
- Currency exchange/conversion
- Withdrawal/deposit operations

## Key Files & Anchors
- Controller: `wallet-service/src/main/java/com/SouthMillion/wallet_service/controller/WalletController.java`
- Service: `wallet-service/src/main/java/com/SouthMillion/wallet_service/service/WalletService.java`
- Entity: `wallet-service/src/main/java/com/SouthMillion/wallet_service/entity/Wallet.java`

## Important APIs
```
GET    /api/wallet/{userId}            - Get wallet balance
POST   /api/wallet/{userId}/deposit     - Deposit currency
POST   /api/wallet/{userId}/withdraw    - Withdraw currency
POST   /api/wallet/{userId}/transfer    - Transfer to another user
GET    /api/wallet/{userId}/history     - Get transaction history
```

## Database Schema
```sql
CREATE TABLE wallets (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) UNIQUE NOT NULL,
  balance BIGINT DEFAULT 0,
  currency_type VARCHAR(20) DEFAULT 'GOLD',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transactions (
  id VARCHAR(36) PRIMARY KEY,
  wallet_id VARCHAR(36) NOT NULL,
  type VARCHAR(20),
  amount BIGINT NOT NULL,
  balance_after BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);
```

## Common Bugs & Patterns
- **Bug 1**: Race condition on balance update
  - Fix: Use pessimistic locking, increment atomic
- **Bug 2**: Negative balance possible
  - Fix: Validate balance >= amount before withdraw
- **Bug 3**: Transaction not logged before payment
  - Fix: Log transaction first, then deduct

## Cross-Service Dependencies
- **user-service**: Validate user exists
- **shop-service**: Deduct balance on purchase
- **payment-service**: Process payments

## Config & Environment
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/wallet_db
    username: root
    password: root

server:
  port: 9021
```

## Command Snippets
```powershell
cd D:\project\serverGame\GameServer\wallet-service
mvn clean install
mvn test
```

## Risk Checklist
- [ ] Balance never goes negative?
- [ ] Transactions logged atomically?
- [ ] Transfer validates both users exist?
- [ ] Currency conversion rate correct?
- [ ] Transaction history immutable?

## Update Log
- 2026-03-21 | Scope: wallet-service | Change: create memory | Why: for all-service memory | Ref: `service-memories/WALLET_SERVICE_MEMORY.md`


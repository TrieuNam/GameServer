# [ServiceName]Handler Documentation

> **Template Version**: 1.0  
> **Copy this template to**: `docs/handlers/[ServiceName]Handler.md`

---

## Overview

**Handler**: `[ServiceName]Handler`  
**Location**: `webSocket-server/src/main/java/com/southMillion/webSocket_server/handler/[service]/[ServiceName]Handler.java`  
**Protocol**: WebSocket Binary (Protobuf)  
**Status**: ⏳ **Not Started** / 🚧 **Partial** / ✅ **Complete**

[Brief description of what this handler does]

---

## Message Flow

```
Client (Browser)
    ↓ WebSocket Binary
    ↓ PB_CS[MessageName]Req (msgId=[REQ_ID])
webSocket-server
    ↓ [ServiceName]Handler.handle()
    ↓ [ServiceName]Feign (Feign Client)
[service-name]-service (REST API)
    ↓ Response
[ServiceName]Handler
    ↓ PB_SC[MessageName]Resp (msgId=[RESP_ID])
Client (Browser)
```

---

## Protocol Definition

### Request: `PB_CS[MessageName]Req` (msgId=[REQ_ID])

**Proto File**: `common-lib/src/main/proto/cs/msg[service].proto`

```protobuf
message PB_CS[MessageName]Req {
    int32 op = 1;           // Operation code
    int64 id = 2;           // Entity ID
    int32 param1 = 3;       // Parameter 1
    repeated int64 list = 4; // List parameter
}
```

**Operations**:
- `op=1`: [Operation 1 description]
- `op=2`: [Operation 2 description]
- `op=3`: [Operation 3 description]

---

### Response: `PB_SC[MessageName]Resp` (msgId=[RESP_ID])

```protobuf
message PB_SC[MessageName]Resp {
    int32 ret = 1;                    // Return code (0=success)
    repeated PB_[Entity]Info list = 2; // Data list
}

message PB_[Entity]Info {
    int64 id = 1;
    int32 field1 = 2;
    int32 field2 = 3;
}
```

---

## Current Implementation

### Handler Structure

```java
@Component
@Slf4j
public class [ServiceName]Handler implements IHandler {
    
    @Autowired
    private [ServiceName]Feign feign;
    
    @Override
    public void handle(PlayerSession session, int msgId, byte[] payload) {
        try {
            Msg[Service].PB_CS[MessageName]Req req = 
                Msg[Service].PB_CS[MessageName]Req.parseFrom(payload);
            
            int operation = req.getOp();
            
            switch (operation) {
                case 1: handleOp1(session, req); break;
                case 2: handleOp2(session, req); break;
                case 3: handleOp3(session, req); break;
                default:
                    log.warn("[[ServiceName]] Invalid operation: {}", operation);
                    sendErrorResponse(session);
            }
        } catch (Exception e) {
            log.error("[[ServiceName]] Error handling request", e);
            sendErrorResponse(session);
        }
    }
    
    private void handleOp1(PlayerSession session, Msg[Service].PB_CS[MessageName]Req req) {
        try {
            // Call service via Feign
            Map<String, Object> result = feign.operation1(session.getRoleId(), params);
            
            // Build response
            Msg[Service].PB_SC[MessageName]Resp.Builder builder = 
                Msg[Service].PB_SC[MessageName]Resp.newBuilder();
            
            if (result.get("success").equals(true)) {
                builder.setRet(0);
                // Add data to response
            } else {
                builder.setRet(1);
            }
            
            session.send([RESP_ID], builder.build().toByteArray());
            
        } catch (Exception e) {
            log.error("[[ServiceName]] Error in handleOp1", e);
            sendErrorResponse(session);
        }
    }
    
    private void sendErrorResponse(PlayerSession session) {
        Msg[Service].PB_SC[MessageName]Resp.Builder builder = 
            Msg[Service].PB_SC[MessageName]Resp.newBuilder();
        builder.setRet(1); // Error code
        session.send([RESP_ID], builder.build().toByteArray());
    }
}
```

---

## Operations Implementation

### OP1: [Operation Name]

**Request Fields**:
- `op = 1`
- `[field]` (required)

**Implementation**:
```java
private void handleOp1(PlayerSession session, Msg[Service].PB_CS[MessageName]Req req) {
    try {
        // Extract parameters
        long param = req.getParam();
        
        // Call Feign client
        Map<String, Object> result = feign.operation1(session.getRoleId(), param);
        
        // Build response
        Msg[Service].PB_SC[MessageName]Resp.Builder builder = 
            Msg[Service].PB_SC[MessageName]Resp.newBuilder();
        
        if (result.get("success").equals(true)) {
            builder.setRet(0);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = 
                (List<Map<String, Object>>) result.get("data");
            
            for (Map<String, Object> item : data) {
                Msg[Service].PB_[Entity]Info.Builder itemBuilder = 
                    Msg[Service].PB_[Entity]Info.newBuilder();
                itemBuilder.setId((Long) item.get("id"));
                itemBuilder.setField1((Integer) item.get("field1"));
                builder.addList(itemBuilder.build());
            }
        } else {
            builder.setRet(1);
        }
        
        session.send([RESP_ID], builder.build().toByteArray());
        
    } catch (Exception e) {
        log.error("[[ServiceName]] Error in handleOp1", e);
        sendErrorResponse(session);
    }
}
```

---

## Feign Client Integration

### [ServiceName]Feign Interface

**Location**: `webSocket-server/src/main/java/com/southMillion/webSocket_server/service/client/[ServiceName]Feign.java`

```java
@FeignClient(name = "[service-name]-service", url = "${services.[service].url:http://localhost:[PORT]}")
public interface [ServiceName]Feign {
    
    @GetMapping("/api/[service]/{roleId}")
    Map<String, Object> get(@PathVariable("roleId") Long roleId);
    
    @PostMapping("/api/[service]/{roleId}/operation1")
    Map<String, Object> operation1(
        @PathVariable("roleId") Long roleId,
        @RequestBody Map<String, Object> request
    );
    
    @PostMapping("/api/[service]/{roleId}/operation2/{id}")
    Map<String, Object> operation2(
        @PathVariable("roleId") Long roleId,
        @PathVariable("id") Long id
    );
}
```

---

## Testing

### WebSocket Test Script

```javascript
// Connect to WebSocket
const ws = new WebSocket('ws://localhost:8080/ws');

// OP1: Test operation 1
const req1 = new PB_CS[MessageName]Req();
req1.setOp(1);
req1.setParam(123);
ws.send(buildMessage([REQ_ID], req1.serializeBinary()));

// OP2: Test operation 2
const req2 = new PB_CS[MessageName]Req();
req2.setOp(2);
req2.setId(456);
ws.send(buildMessage([REQ_ID], req2.serializeBinary()));

// Listen for response
ws.onmessage = (event) => {
    const response = PB_SC[MessageName]Resp.deserializeBinary(event.data);
    console.log('Response:', response.toObject());
};
```

---

## Status Checklist

- [ ] OP1: [Operation 1] - Implemented
- [ ] OP2: [Operation 2] - Implemented
- [ ] OP3: [Operation 3] - Implemented
- [ ] Error handling - Implemented
- [ ] Feign client integration - Complete
- [ ] WebSocket testing - Done
- [ ] E2E testing - Done

---

## Next Steps

1. Implement missing operations
2. Test with real service
3. Create frontend client
4. E2E test

---

## Related Documentation

- [[ServiceName] Service Backend](../services/[service-name]-service.md)
- [[ServiceName]Service.ts Client](../clients/[ServiceName]Service.md)
- [WebSocket Protocol Guide](../../WEBSOCKET_PROTOCOL.md)

---

*Last Updated: [DATE]*

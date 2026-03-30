# [ServiceName]Service.ts Client Documentation

> **Template Version**: 1.0  
> **Copy this template to**: `docs/clients/[ServiceName]Service.md`

---

## Overview

**Client**: `[ServiceName]Service.ts`  
**Location**: `client/LineR/src/services/[ServiceName]Service.ts`  
**Protocol**: WebSocket Binary (Protobuf)  
**Status**: ⏳ **Not Started** / 🚧 **In Progress** / ✅ **Complete**

TypeScript client wrapper for [service description] via WebSocket.

---

## Architecture

```
React Component
    ↓ Import [ServiceName]Service
[ServiceName]Service.ts
    ↓ WebSocketService
    ↓ Binary Protocol (msgId=[REQ_ID])
webSocket-server
    ↓ [ServiceName]Handler
[service-name]-service
```

---

## Implementation

### File: `client/LineR/src/services/[ServiceName]Service.ts`

```typescript
import { PB_CS[MessageName]Req, PB_SC[MessageName]Resp } from '../proto/msg[service]_pb';
import { WebSocketService } from './WebSocketService';

/**
 * [ServiceName] Service Client
 * [Description of what this service does]
 */
export class [ServiceName]Service {
    private wsService: WebSocketService;
    
    constructor(wsService: WebSocketService) {
        this.wsService = wsService;
    }
    
    /**
     * OP1: [Operation 1 description]
     * @param param - [Parameter description]
     * @returns Promise<PB_SC[MessageName]Resp>
     */
    async operation1(param: [Type]): Promise<PB_SC[MessageName]Resp> {
        const req = new PB_CS[MessageName]Req();
        req.setOp(1);
        req.setParam(param);
        
        return this.wsService.sendMessage([REQ_ID], req.serializeBinary());
    }
    
    /**
     * OP2: [Operation 2 description]
     * @param id - [Parameter description]
     * @returns Promise<PB_SC[MessageName]Resp>
     */
    async operation2(id: number): Promise<PB_SC[MessageName]Resp> {
        const req = new PB_CS[MessageName]Req();
        req.setOp(2);
        req.setId(id);
        
        return this.wsService.sendMessage([REQ_ID], req.serializeBinary());
    }
    
    /**
     * OP3: [Operation 3 description]
     * @param param - [Parameter description]
     * @param list - [Parameter description]
     * @returns Promise<PB_SC[MessageName]Resp>
     */
    async operation3(param: [Type], list: number[]): Promise<PB_SC[MessageName]Resp> {
        const req = new PB_CS[MessageName]Req();
        req.setOp(3);
        req.setParam(param);
        req.setListList(list);
        
        return this.wsService.sendMessage([REQ_ID], req.serializeBinary());
    }
}
```

---

## React Integration

### Hook: `use[ServiceName]Service.ts`

```typescript
import { useState, useEffect } from 'react';
import { [ServiceName]Service } from '../services/[ServiceName]Service';
import { useWebSocket } from './useWebSocket';

interface [Entity]Info {
    id: number;
    field1: number;
    field2: string;
}

export const use[ServiceName]Service = () => {
    const wsService = useWebSocket();
    const [service] = useState(() => new [ServiceName]Service(wsService));
    const [items, setItems] = useState<[Entity]Info[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    
    // Load items on mount
    useEffect(() => {
        loadItems();
    }, []);
    
    const loadItems = async () => {
        setLoading(true);
        setError(null);
        try {
            const response = await service.operation1(param);
            if (response.getRet() === 0) {
                const itemList = response.getListList().map(item => ({
                    id: item.getId(),
                    field1: item.getField1(),
                    field2: item.getField2()
                }));
                setItems(itemList);
            } else {
                setError('Failed to load items');
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };
    
    const performOperation2 = async (id: number) => {
        setLoading(true);
        setError(null);
        try {
            const response = await service.operation2(id);
            if (response.getRet() === 0) {
                await loadItems(); // Refresh list
                return true;
            } else {
                setError('Operation failed');
                return false;
            }
        } catch (err) {
            setError(err.message);
            return false;
        } finally {
            setLoading(false);
        }
    };
    
    return {
        items,
        loading,
        error,
        loadItems,
        performOperation2
    };
};
```

---

## Component Example

### `[ServiceName]Panel.tsx`

```typescript
import React from 'react';
import { use[ServiceName]Service } from '../hooks/use[ServiceName]Service';

export const [ServiceName]Panel: React.FC = () => {
    const { 
        items, 
        loading, 
        error, 
        loadItems,
        performOperation2
    } = use[ServiceName]Service();
    
    const handleOperation = async (id: number) => {
        const success = await performOperation2(id);
        if (success) {
            alert('Operation successful!');
        }
    };
    
    if (loading) return <div>Loading...</div>;
    if (error) return <div>Error: {error}</div>;
    
    return (
        <div className="[service]-panel">
            <h2>[Service Name] ({items.length})</h2>
            
            <button onClick={loadItems}>
                Refresh
            </button>
            
            <div className="[service]-list">
                {items.map(item => (
                    <div key={item.id} className="[service]-card">
                        <h3>Item #{item.id}</h3>
                        <p>Field 1: {item.field1}</p>
                        <p>Field 2: {item.field2}</p>
                        
                        <button onClick={() => handleOperation(item.id)}>
                            Perform Action
                        </button>
                    </div>
                ))}
            </div>
        </div>
    );
};
```

---

## Testing

### Unit Test: `[ServiceName]Service.test.ts`

```typescript
import { [ServiceName]Service } from '../services/[ServiceName]Service';
import { WebSocketService } from '../services/WebSocketService';

describe('[ServiceName]Service', () => {
    let service: [ServiceName]Service;
    let mockWsService: jest.Mocked<WebSocketService>;
    
    beforeEach(() => {
        mockWsService = {
            sendMessage: jest.fn()
        } as any;
        
        service = new [ServiceName]Service(mockWsService);
    });
    
    it('should perform operation 1', async () => {
        const mockResponse = {
            getRet: () => 0,
            getListList: () => []
        };
        
        mockWsService.sendMessage.mockResolvedValue(mockResponse as any);
        
        const result = await service.operation1(param);
        
        expect(mockWsService.sendMessage).toHaveBeenCalledWith(
            [REQ_ID],
            expect.any(Uint8Array)
        );
        expect(result.getRet()).toBe(0);
    });
    
    it('should perform operation 2', async () => {
        const mockResponse = {
            getRet: () => 0
        };
        
        mockWsService.sendMessage.mockResolvedValue(mockResponse as any);
        
        const result = await service.operation2(123);
        
        expect(result.getRet()).toBe(0);
    });
});
```

---

## E2E Test Flow

```typescript
describe('[ServiceName] System E2E', () => {
    it('should complete full workflow', async () => {
        const wsService = new WebSocketService('ws://localhost:8080/ws');
        const service = new [ServiceName]Service(wsService);
        
        // 1. Get initial data
        let response = await service.operation1(param);
        expect(response.getRet()).toBe(0);
        
        // 2. Perform operation 2
        response = await service.operation2(123);
        expect(response.getRet()).toBe(0);
        
        // 3. Verify result
        response = await service.operation1(param);
        expect(response.getListList().length).toBeGreaterThan(0);
    });
});
```

---

## Protocol Reference

### Request (Client → Server)

```typescript
// msgId: [REQ_ID]
// Message: PB_CS[MessageName]Req

interface PB_CS[MessageName]Req {
    op: number;         // Operation code
    id?: number;        // Optional ID
    param?: number;     // Optional parameter
    list?: number[];    // Optional list
}
```

### Response (Server → Client)

```typescript
// msgId: [RESP_ID]
// Message: PB_SC[MessageName]Resp

interface PB_SC[MessageName]Resp {
    ret: number;        // 0=success, 1=error
    list: PB_[Entity]Info[];
}

interface PB_[Entity]Info {
    id: number;
    field1: number;
    field2: string;
}
```

---

## Status Checklist

- [ ] [ServiceName]Service.ts - Created
- [ ] use[ServiceName]Service.ts hook - Created
- [ ] [ServiceName]Panel.tsx component - Created
- [ ] Unit tests - Written
- [ ] E2E tests - Written
- [ ] Integration tested - Done

---

## Next Steps

1. Create TypeScript service class
2. Create React hook
3. Create UI component
4. Write tests
5. Integrate with app

---

## Related Documentation

- [[ServiceName] Service Backend](../services/[service-name]-service.md)
- [[ServiceName]Handler WebSocket](../handlers/[ServiceName]Handler.md)
- [WebSocket Protocol Guide](../../WEBSOCKET_PROTOCOL.md)

---

*Last Updated: [DATE]*

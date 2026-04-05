# Service Doctor Copilot Prompt

Service: friend-service
Display name: Friend Service
Phase: P3
Port: 8450
Current service status: STOPPED
Current doctor status: FIXING
Detected error type: -
Summary: Service is running normally

## Recent logs
```text
2026-04-05T20:41:04.629+07:00  INFO 8180 --- [friend-service] [           main] c.S.c.o.JvmArgumentsSuggester            :    resources:
2026-04-05T20:41:04.629+07:00  INFO 8180 --- [friend-service] [           main] c.S.c.o.JvmArgumentsSuggester            :      limits:
2026-04-05T20:41:04.629+07:00  INFO 8180 --- [friend-service] [           main] c.S.c.o.JvmArgumentsSuggester            :        memory: "96Mi"
2026-04-05T20:41:04.629+07:00  INFO 8180 --- [friend-service] [           main] c.S.c.o.JvmArgumentsSuggester            :      requests:
2026-04-05T20:41:04.629+07:00  INFO 8180 --- [friend-service] [           main] c.S.c.o.JvmArgumentsSuggester            :        memory: "48Mi"
2026-04-05T20:41:04.629+07:00  INFO 8180 --- [friend-service] [           main] c.S.c.o.JvmArgumentsSuggester            : â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
2026-04-05T20:41:54.968+07:00  INFO 8180 --- [friend-service] [    virtual-123] c.S.f.controller.FriendController        : REST API: Get friend list - roleId=1
2026-04-05T20:41:54.992+07:00  INFO 8180 --- [friend-service] [    virtual-123] c.S.f.service.FriendService              : Getting friend list: roleId=1
2026-04-05T20:41:55.282+07:00  INFO 8180 --- [friend-service] [    virtual-123] c.S.f.service.FriendService              : Friend list retrieved: roleId=1, count=0
2026-04-05T20:41:55.376+07:00  INFO 8180 --- [friend-service] [    virtual-129] c.S.f.controller.FriendController        : REST API: Get received requests - roleId=1
2026-04-05T20:41:55.380+07:00  INFO 8180 --- [friend-service] [    virtual-129] c.S.f.service.FriendService              : Getting received requests: roleId=1
2026-04-05T20:45:58.676+07:00  INFO 8180 --- [friend-service] [rap-executor-%d] c.n.d.s.r.aws.ConfigClusterResolver      : Resolving eureka endpoints via configuration
```

## Task
- find the root cause
- propose the smallest safe fix
- do not change unrelated files
- keep current behavior intact
- ensure the relevant Maven build passes after the fix

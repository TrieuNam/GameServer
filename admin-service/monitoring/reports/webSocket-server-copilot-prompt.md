# Service Doctor Copilot Prompt

Service: webSocket-server
Display name: WebSocket Server
Phase: P0
Port: 8094
Current service status: STOPPED
Current doctor status: FIXING
Detected error type: -
Summary: Service is running normally

## Recent logs
```text
2026-04-05T20:46:44.518+07:00  INFO 19808 --- [websocket-server] [ntainer#1-0-C-1] c.S.w.handler.task.TaskHandler           : [Task] Pushed task state — roleId=1 taskId=8 progress=1 (status check enforced)
2026-04-05T20:46:44.518+07:00 DEBUG 19808 --- [websocket-server] [ntainer#1-0-C-1] c.S.w.handler.task.TaskHandler           : [Task] Sent 1452 id=8 progress=1 → roleId=1
2026-04-05T20:46:44.643+07:00 DEBUG 19808 --- [websocket-server] [    virtual-642] feign.template.Template                  : Explicit slash decoding specified, decoding all slashes in uri
2026-04-05T20:46:44.643+07:00 DEBUG 19808 --- [websocket-server] [    virtual-643] feign.template.Template                  : Explicit slash decoding specified, decoding all slashes in uri
2026-04-05T20:46:44.643+07:00 DEBUG 19808 --- [websocket-server] [    virtual-642] c.S.w.service.FeignAuthInterceptor       : [feign] NO token for GET /info?roleId=1
2026-04-05T20:46:44.643+07:00 DEBUG 19808 --- [websocket-server] [    virtual-643] c.S.w.service.FeignAuthInterceptor       : [feign] add Authorization (len=329, startsWithBearer=false) for GET /api/role/by-user/99130213-d10c-4553-8920-3e568fd7ad92
2026-04-05T20:46:44.660+07:00 DEBUG 19808 --- [websocket-server] [    virtual-644] feign.template.Template                  : Explicit slash decoding specified, decoding all slashes in uri
2026-04-05T20:46:44.660+07:00 DEBUG 19808 --- [websocket-server] [    virtual-644] feign.template.Template                  : Explicit slash decoding specified, decoding all slashes in uri
2026-04-05T20:46:44.660+07:00 DEBUG 19808 --- [websocket-server] [    virtual-644] c.S.w.service.FeignAuthInterceptor       : [feign] add Authorization (len=329, startsWithBearer=false) for GET /api/other-role/99130213-d10c-4553-8920-3e568fd7ad92?roleId=1
2026-04-05T20:46:49.271+07:00  INFO 19808 --- [websocket-server] [rap-executor-%d] c.n.d.s.r.aws.ConfigClusterResolver      : Resolving eureka endpoints via configuration
2026-04-05T20:46:49.365+07:00 DEBUG 19808 --- [websocket-server] [adiness-monitor] c.S.w.config.StartupDependencyReadiness  : [startup] world-service has no actuator health endpoint at http://192.168.1.23:8370/actuator/health, treating discovered instance as reachable
2026-04-05T20:46:49.379+07:00 DEBUG 19808 --- [websocket-server] [adiness-monitor] c.S.w.config.StartupDependencyReadiness  : [startup] config-service has no actuator health endpoint at http://192.168.1.23:8888/actuator/health, treating discovered instance as reachable
```

## Task
- find the root cause
- propose the smallest safe fix
- do not change unrelated files
- keep current behavior intact
- ensure the relevant Maven build passes after the fix

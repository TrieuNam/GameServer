common/proto module

Purpose
- Provide a small Gradle module that generates Java classes from the existing protobuf files located in the C++ source tree.

Why not copy proto files?
- To avoid duplication and keep a single source of truth, the Gradle module references the proto files in the original C++ repo path: `开箱h5/server/server/src/servercommon/proto` and `.../protobuf`.

How to generate Java classes
1. Install Gradle (or use the Gradle wrapper if added later).
2. From repository root run (PowerShell):

```powershell
cd D:\project\serverGame\GameServer\common\proto
gradle listProtoFiles
gradle build
```

3. Generated Java classes will be in `build/generated/source/proto/main/java` and gRPC classes in `build/generated/source/proto/main/grpc`.

Notes
- You may later move the proto files into this module (e.g., `common/proto/src/main/proto`) if you want Java to be the canonical source.
- If any `.proto` uses C++-only options, update them or create a trimmed copy in `common/proto/src/main/proto`.

# Monitoring Folder

Thư mục này dùng cho Service Doctor MVP.

## Hiện có
- `service-registry.json`: registry mẫu cho service watcher / build verify.
- `reports/`: nơi `admin-service` tự ghi snapshot JSON cho từng service doctor session.
- `Invoke-CopilotRepair.ps1`: scaffold chuẩn bị prompt cho Copilot CLI.
- `Verify-Build.ps1`: script verify build cục bộ.

## Cách chạy trên Windows
Do Execution Policy của PowerShell có thể đang chặn script `.ps1`, hãy chạy theo mẫu này:

```powershell
powershell -ExecutionPolicy Bypass -File .\monitoring\Verify-Build.ps1 -WorkingDirectory "D:\project\serverGame\GameServer\admin-service"
powershell -ExecutionPolicy Bypass -File .\monitoring\Invoke-CopilotRepair.ps1 -PromptFile ".\monitoring\reports\sample-prompt.md"
```

## Lưu ý
- `admin-service` hiện đã cấu hình sẵn command template thật:

```yml
doctor:
  copilot:
    enabled: true
    command-template: "$prompt = Get-Content -Raw '{promptFile}'; gh copilot -p $prompt"
```

- nếu máy chưa có `gh` trong `PATH`, hệ thống sẽ tự rơi về **prepare prompt + audit file**;
- để dùng thật, cài và đăng nhập GitHub CLI:

```powershell
gh auth login --web
gh copilot -- --help
```

> Lần chạy `gh copilot` đầu tiên có thể tự tải runtime Copilot CLI preview về máy.

## Giai đoạn sau
- thêm PowerShell watcher;
- hoàn thiện adapter gọi GitHub Copilot CLI;
- thêm apply-patch / rollback flow an toàn.

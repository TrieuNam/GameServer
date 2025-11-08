# Phase P4 — Nhóm Khác (Analytics, Notifications, Report, Files, Scheduler...)

Mục tiêu
- Port auxiliary services: analytics, notifications, reporting, file handling, scheduler, localization, moderation, IAP verification, anti-cheat.

Service mapping & notes
- `analytics-service` — collect events and push to analytics pipeline (Kafka -> consumer -> clickhouse / data lake)
- `notification-service` — push/email/SMS (external providers)
- `report-service` — CSV/Excel exports (offload heavy report generation to background jobs)
- `file-service` — upload assets, sign URL, store in object storage (S3-compatible)
- `scheduler-service` — cron tasks, season resets (use Quartz or Spring Scheduler)
- `localization-service` — l10n strings and management
- `moderation-service` — user reports, actions (mute/kick)
- `iap-verify-service` — iap receipts, webhook handling -> wallet
- `anti-cheat-service` — anomaly detection (consumer of combat logs/telemetry)

Steps
1. Build `analytics-service` as a Kafka consumer that writes to your chosen analytics store.
2. Notification-service: integrate with provider SDKs and provide async job queues (Kafka or Redis Streams).
3. Report-service: implement export jobs (worker pool) and signed URL for download (file-service).
4. Scheduler-service: central cron coordinator; use distributed locks (Redis) to avoid duplicate runs.

Verification
- Analytics pipeline: produce synthetic events from a test service -> verify storage and dashboards.
- Notifications: send test email/push via provider sandbox.
- Reports: generate a sample CSV and verify download link works.

Risks
- External provider integrations (email, SMS, IAP) need sandbox credentials and careful error handling.
- File storage security (signed URLs) and quotas.

Artifacts
- `services/analytics-service`, `services/notification-service`, `services/report-service`, `services/file-service`, `services/scheduler-service` skeletons.

Next
- I can scaffold `analytics-service` (consumer + example sink) or create the `scheduler-service` with distributed lock example. Choose one.
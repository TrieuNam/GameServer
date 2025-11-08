# Proto replacement workflow

This document explains how to compare `.proto` files in `common-lib` with the centralized `common/proto` copy, review differences, and gradually replace files.

Script provided
- `scripts/compare_protos.ps1` — compares proto files, generates `docs/proto-compare-report.csv`, stores diffs under `%TEMP%/proto-diffs`, and can copy/replace files with `-Replace`.

Typical workflow
1. Dry run and report
   ```powershell
   cd D:\project\serverGame\GameServer
   .\scripts\compare_protos.ps1 -LibPath .\common-lib\src\main\proto -CommonProtoPath .\common\proto\src\main\proto -ReportPath .\docs\proto-compare-report.csv
   ```
   - Open `docs/proto-compare-report.csv` to inspect statuses: `Identical`, `Different`, `Missing`, `CommonOnly`.
   - See diffs for `Different` entries in `%TEMP%/proto-diffs`.

2. Replace a single file (safe)
   - After checking the diff, to replace one file manually, copy the file from `common-lib` to `common/proto/src/main/proto` and commit.

3. Replace in batch (automated)
   - To automatically copy missing/different files and back up originals in `common/proto`:
   ```powershell
   .\scripts\compare_protos.ps1 -LibPath .\common-lib\src\main\proto -CommonProtoPath .\common\proto\src\main\proto -ReportPath .\docs\proto-compare-report.csv -Replace
   ```
   - The script will copy files and back up replaced originals into `backup_<timestamp>` under the `common/proto` folder.
   - Review changes and commit.

4. Commit workflow
   - After replacing, run:
   ```powershell
   git status
   git add common/proto/src/main/proto
   git commit -m "chore(proto): replace proto files from common-lib"
   ```

Notes & cautions
- Always run the dry-run report and review diffs before using `-Replace`.
- The script uses `git diff --no-index` when git is available for diffs; otherwise it falls back to `fc` on Windows.
- Backups are stored automatically by the script before overwriting.

If you'd like, I can run a dry-run now and attach the generated CSV report contents here.
$file = "docker-compose.local-full.yml"
$content = [System.IO.File]::ReadAllText($file)

# Replace pattern: add innodb-use-native-aio=0 after collation-server line
$newContent = $content -replace '(--collation-server=utf8mb4_unicode_ci)\r?\n(\s+)healthcheck:', '$1`n$2- "--innodb-use-native-aio=0"`n$2healthcheck:'

[System.IO.File]::WriteAllText($file, $newContent)

Write-Host "Applied MySQL AIO fix to all services"

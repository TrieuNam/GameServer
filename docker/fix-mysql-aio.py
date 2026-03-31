#!/usr/bin/env python3

import re

# Read the docker-compose file
with open('docker-compose.local-full.yml', 'r') as f:
    content = f.read()

# Replace pattern: add --innodb-use-native-aio=0 after --collation-server line
pattern = r'(--collation-server=utf8mb4_unicode_ci)'
replacement = r'\1\n      - "--innodb-use-native-aio=0"'

new_content = re.sub(pattern, replacement, content)

# Write back
with open('docker-compose.local-full.yml', 'w') as f:
    f.write(new_content)

print("MySQL AIO fix applied to all services")

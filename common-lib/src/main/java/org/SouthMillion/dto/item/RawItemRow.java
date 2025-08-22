package org.SouthMillion.dto.item;

import java.util.Map;

public record RawItemRow(
        int id,
        String topNode,
        String sourceFile,
        Map<String, String> fields // tất cả cặp key->string
) {}
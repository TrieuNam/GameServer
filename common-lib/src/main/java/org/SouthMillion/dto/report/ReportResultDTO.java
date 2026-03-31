package org.SouthMillion.dto.report;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReportResultDTO {
    private String status;
    private String decodedData;
}

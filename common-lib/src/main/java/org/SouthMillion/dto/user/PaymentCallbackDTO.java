package org.SouthMillion.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PaymentCallbackDTO {
    @JsonProperty("userId")
    private Long userId;
    @JsonProperty("amount")
    private int amount;
    @JsonProperty("productId")
    private String productId;
    @JsonProperty("transactionId")
    private String transactionId;
}

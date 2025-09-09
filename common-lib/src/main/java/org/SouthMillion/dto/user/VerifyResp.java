package org.SouthMillion.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VerifyResp {
    private boolean ok;
    private String userId;

    /**
     * Một số user-service trả "username", số khác trả "account".
     * Để tương thích code hiện tại của bạn, mình giữ cả hai field.
     */
    private String username; // optional
    private String account;  // optional
}
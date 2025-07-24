package org.SouthMillion.dto.globalserver;

import lombok.*;
import org.SouthMillion.dto.ItemDTO;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class MailDTO {
    private Integer id;
    private Integer mailType;
    private Long recvTime;
    private Boolean isRead;
    private Boolean isFetched;
    private String subject;
    private String content;
    private List<ItemDTO> itemList;
    // getters, setters
}
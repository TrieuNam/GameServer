package org.SouthMillion.dto.bag;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class BagDTOs {

public static class GrantReq {
@NotNull(message = "User ID must not be null")
private Long userId;

@NotNull(message = "Item list must not be null")
@Size(min = 1, message = "Item list must contain at least one item")
private List<@NotNull(message = "Item must not be null") Long> itemIds;

// Getters and Setters
public Long getUserId() {
return userId;
}
public void setUserId(Long userId) {
this.userId = userId;
}
public List<Long> getItemIds() {
return itemIds;
}
public void setItemIds(List<Long> itemIds) {
this.itemIds = itemIds;
}
}

public static class ItemView {
private Long itemId;
private Integer quantity;

public Long getItemId() {
return itemId;
}
public void setItemId(Long itemId) {
this.itemId = itemId;
}
public Integer getQuantity() {
return quantity;
}
public void setQuantity(Integer quantity) {
this.quantity = quantity;
}
}

public static class BagResp {
private Long bagId;
private List<ItemView> items;

public Long getBagId() {
return bagId;
}
public void setBagId(Long bagId) {
this.bagId = bagId;
}
public List<ItemView> getItems() {
return items;
}
public void setItems(List<ItemView> items) {
this.items = items;
}
}
}
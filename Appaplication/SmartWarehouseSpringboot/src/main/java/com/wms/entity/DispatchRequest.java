package com.wms.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DispatchRequest {
    private Integer bigWarehouseId;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String deadline;
    private Integer adminId;
}

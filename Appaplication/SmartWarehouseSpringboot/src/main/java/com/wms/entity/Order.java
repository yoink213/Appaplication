package com.wms.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {
    private Integer id;
    private String orderNo;
    private String smallItemCode;
    private Integer bigWarehouseId;
    private Integer quantity;
    private BigDecimal totalPrice;
    private LocalDateTime deadline;
    private Integer status;      // 0-待接单 1-已接单 2-已取货 3-已送达 4-已完成 5-已拒绝
    private Integer adminId;
    private Integer workerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 扩展字段（用于 JOIN 查询）
    private String itemName;
    private String itemCodeLarge;
    private String location;
}

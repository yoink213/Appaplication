package com.wms.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BigWarehouse {
    private Integer id;
    private String code;
    private String name;
    private String category;
    private Integer quantity;
    private BigDecimal price;
    private String location;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

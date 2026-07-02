package com.wms.entity;

import lombok.Data;

@Data
public class CategoryStats {
    private String category;
    private Integer totalQuantity;
    private Integer itemCount;
}

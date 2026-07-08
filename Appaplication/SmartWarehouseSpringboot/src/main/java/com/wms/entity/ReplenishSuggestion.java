package com.wms.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ReplenishSuggestion {
    private Integer id;
    private String code;
    private String name;
    private String category;
    private Integer currentQuantity;
    private Integer suggestedQuantity;
    private BigDecimal estimatedCost;
    private String reason;
}

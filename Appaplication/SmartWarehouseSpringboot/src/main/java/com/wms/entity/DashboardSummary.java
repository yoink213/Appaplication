package com.wms.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DashboardSummary {
    private Integer totalItemTypes;
    private Integer totalStock;
    private BigDecimal totalStockValue;
    private Integer todayOrders;
    private Integer pendingOrders;
    private Integer completedOrders;
    private Integer lowStockCount;
    private Integer workerCount;
    private BigDecimal deliveryRate;
}

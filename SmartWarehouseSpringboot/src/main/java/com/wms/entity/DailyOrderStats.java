package com.wms.entity;

import lombok.Data;

@Data
public class DailyOrderStats {
    private String date;
    private Integer orderCount;
}

package com.wms.mapper;

import com.wms.entity.CategoryStats;
import com.wms.entity.DailyOrderStats;
import com.wms.entity.BigWarehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StatsMapper {

    int countTotalItemTypes();

    int countTotalStock();

    java.math.BigDecimal sumTotalStockValue();

    int countTodayOrders();

    int countPendingOrders();

    int countCompletedOrders();

    int countWorkers();

    List<CategoryStats> getCategoryStats();

    List<DailyOrderStats> getDailyOrderTrend(@Param("days") Integer days);

    List<BigWarehouse> getLowStockItems(@Param("threshold") Integer threshold);

    int getDefaultThreshold();
}

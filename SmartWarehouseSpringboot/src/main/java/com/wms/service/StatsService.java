package com.wms.service;

import com.wms.entity.BigWarehouse;
import com.wms.entity.CategoryStats;
import com.wms.entity.DailyOrderStats;
import com.wms.entity.DashboardSummary;
import com.wms.entity.ReplenishSuggestion;
import com.wms.mapper.BigWarehouseMapper;
import com.wms.mapper.StatsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatsService {

    @Autowired
    private StatsMapper statsMapper;

    @Autowired
    private BigWarehouseMapper bigWarehouseMapper;

    public DashboardSummary getSummary() {
        DashboardSummary summary = new DashboardSummary();
        summary.setTotalItemTypes(statsMapper.countTotalItemTypes());
        summary.setTotalStock(statsMapper.countTotalStock());
        summary.setTotalStockValue(statsMapper.sumTotalStockValue());
        summary.setTodayOrders(statsMapper.countTodayOrders());
        summary.setPendingOrders(statsMapper.countPendingOrders());
        summary.setCompletedOrders(statsMapper.countCompletedOrders());
        summary.setLowStockCount(getLowStockItems().size());
        summary.setWorkerCount(statsMapper.countWorkers());

        int total = summary.getPendingOrders() + summary.getCompletedOrders();
        if (total > 0) {
            double rate = (double) summary.getCompletedOrders() / total * 100;
            summary.setDeliveryRate(BigDecimal.valueOf(rate).setScale(1, RoundingMode.HALF_UP));
        } else {
            summary.setDeliveryRate(BigDecimal.ZERO);
        }

        return summary;
    }

    public List<CategoryStats> getCategoryStats() {
        return statsMapper.getCategoryStats();
    }

    public List<DailyOrderStats> getDailyOrderTrend(Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }
        return statsMapper.getDailyOrderTrend(days);
    }

    public List<BigWarehouse> getLowStockItems() {
        int threshold = statsMapper.getDefaultThreshold();
        return statsMapper.getLowStockItems(threshold);
    }

    public List<ReplenishSuggestion> getReplenishSuggestions() {
        List<BigWarehouse> lowStockItems = getLowStockItems();
        List<ReplenishSuggestion> suggestions = new ArrayList<>();

        for (BigWarehouse item : lowStockItems) {
            ReplenishSuggestion s = new ReplenishSuggestion();
            s.setId(item.getId());
            s.setCode(item.getCode());
            s.setName(item.getName());
            s.setCategory(item.getCategory());
            s.setCurrentQuantity(item.getQuantity());

            int suggested = calculateSuggestedQuantity(item);
            s.setSuggestedQuantity(suggested);
            s.setEstimatedCost(item.getPrice().multiply(BigDecimal.valueOf(suggested)));

            String reason;
            if (item.getQuantity() < 50) {
                reason = "紧急：库存严重不足，建议立即补货";
            } else if (item.getQuantity() < 200) {
                reason = "库存偏低，建议近期补货";
            } else {
                reason = "库存低于平均水平，建议关注";
            }
            s.setReason(reason);

            suggestions.add(s);
        }

        return suggestions;
    }

    private int calculateSuggestedQuantity(BigWarehouse item) {
        int avgDaily = Math.max(1, item.getQuantity() / 30);
        int safetyStock = avgDaily * 7;
        int target = avgDaily * 30;
        int need = target - item.getQuantity();
        return Math.max(need, safetyStock);
    }
}

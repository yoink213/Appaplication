package com.wms.controller;

import com.wms.entity.BigWarehouse;
import com.wms.entity.CategoryStats;
import com.wms.entity.DailyOrderStats;
import com.wms.entity.DashboardSummary;
import com.wms.entity.ReplenishSuggestion;
import com.wms.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping("/summary")
    public DashboardSummary getSummary() {
        return statsService.getSummary();
    }

    @GetMapping("/category")
    public List<CategoryStats> getCategoryStats() {
        return statsService.getCategoryStats();
    }

    @GetMapping("/trend")
    public List<DailyOrderStats> getTrend(@RequestParam(required = false, defaultValue = "7") Integer days) {
        return statsService.getDailyOrderTrend(days);
    }

    @GetMapping("/lowStock")
    public List<BigWarehouse> getLowStockItems() {
        return statsService.getLowStockItems();
    }

    @GetMapping("/replenish")
    public Map<String, Object> getReplenishSuggestions() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ReplenishSuggestion> list = statsService.getReplenishSuggestions();
            result.put("code", 200);
            result.put("data", list);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}

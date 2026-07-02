package com.wms.controller;

import com.wms.service.WorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private WorkerService workerService;

    @PostMapping("/audit")
    public Map<String, Object> auditOrder(
            @RequestParam Integer orderId,
            @RequestParam String action) {
        Map<String, Object> result = new HashMap<>();
        try {
            String message = workerService.auditOrder(orderId, action);
            result.put("code", 200);
            result.put("message", message);
        } catch (Exception e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        }
        return result;
    }
}

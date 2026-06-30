package com.wms.controller;

import com.wms.service.WorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/worker")
@CrossOrigin(origins = "*")
public class WorkerController {

    @Autowired
    private WorkerService workerService;

    @PostMapping("/action")
    public Map<String, Object> executeAction(
            @RequestParam String action,
            @RequestParam Integer orderId,
            @RequestParam Integer workerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String message;
            switch (action) {
                case "accept":
                    message = workerService.acceptOrder(orderId, workerId);
                    break;
                case "pickup":
                    message = workerService.pickupOrder(orderId, workerId);
                    break;
                case "deliver":
                    message = workerService.deliverOrder(orderId, workerId);
                    break;
                default:
                    result.put("code", 400);
                    result.put("message", "未知操作");
                    return result;
            }
            result.put("code", 200);
            result.put("message", message);
        } catch (Exception e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        }
        return result;
    }
}

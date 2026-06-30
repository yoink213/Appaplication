package com.wms.controller;

import com.wms.entity.BigWarehouse;
import com.wms.entity.DispatchRequest;
import com.wms.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    @GetMapping("/warehouse")
    public List<BigWarehouse> getWarehouseItems() {
        return warehouseService.getAllItems();
    }

    @PostMapping("/warehouse")
    public ResponseEntity<Map<String, Object>> dispatchTask(@RequestBody DispatchRequest request) {
        Map<String, Object> body = new HashMap<>();
        try {
            String orderNo = warehouseService.dispatchTask(
                request.getBigWarehouseId(),
                request.getQuantity(),
                request.getTotalPrice().doubleValue(),
                request.getDeadline(),
                request.getAdminId()
            );
            body.put("code", 200);
            body.put("message", "生成订单（小仓任务）成功，订单号 " + orderNo);
            return ResponseEntity.ok(body);
        } catch (RuntimeException e) {
            body.put("code", 400);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        } catch (Exception e) {
            body.put("code", 500);
            body.put("message", "服务器内部异常");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @GetMapping("/warehouse/nextCode")
    public Map<String, Object> getNextCode() {
        Map<String, Object> result = new HashMap<>();
        String nextCode = warehouseService.getNextCode();
        result.put("code", 200);
        result.put("data", nextCode);
        return result;
    }

    @PostMapping("/warehouse/add")
    public ResponseEntity<Map<String, Object>> addItem(@RequestBody BigWarehouse item) {
        Map<String, Object> body = new HashMap<>();
        try {
            BigWarehouse newItem = warehouseService.addItem(item);
            body.put("code", 200);
            body.put("message", "添加成功");
            body.put("data", newItem);
            return ResponseEntity.ok(body);
        } catch (RuntimeException e) {
            body.put("code", 400);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        } catch (Exception e) {
            body.put("code", 500);
            body.put("message", "添加失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}

package com.wms.controller;

import com.wms.entity.Order;
import com.wms.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public List<Order> getOrders(
            @RequestParam(required = false, defaultValue = "1") Integer role,
            @RequestParam(required = false) Integer workerId) {
        log.info("GET /api/orders called, role={}, workerId={}", role, workerId);
        return orderService.getOrders(role, workerId);
    }

    // 订单修改：同时支持 POST /api/orders/update 和 PUT /api/orders
    @PostMapping("/orders/update")
    public ResponseEntity<Map<String, Object>> updateOrderPost(@RequestBody Map<String, Object> request) {
        log.info("POST /api/orders/update called, body={}", request);
        return handleUpdate(request);
    }

    @PutMapping("/orders")
    public ResponseEntity<Map<String, Object>> updateOrderPut(@RequestBody Map<String, Object> request) {
        log.info("PUT /api/orders called, body={}", request);
        return handleUpdate(request);
    }

    private ResponseEntity<Map<String, Object>> handleUpdate(Map<String, Object> request) {
        Map<String, Object> body = new HashMap<>();
        try {
            Integer orderId = Integer.parseInt(request.get("orderId").toString());
            Double totalPrice = Double.parseDouble(request.get("totalPrice").toString());
            String deadline = request.get("deadline").toString();

            boolean success = orderService.updateOrder(orderId, totalPrice, deadline);
            if (success) {
                body.put("code", 200);
                body.put("message", "订单修改成功");
                log.info("订单修改成功, orderId={}", orderId);
                return ResponseEntity.ok(body);
            } else {
                body.put("code", 400);
                body.put("message", "订单状态已改变，无法修改");
                log.warn("订单修改失败: 状态不匹配, orderId={}", orderId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }
        } catch (NumberFormatException e) {
            body.put("code", 400);
            body.put("message", "参数格式错误");
            log.error("订单修改参数格式错误", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        } catch (Exception e) {
            body.put("code", 500);
            body.put("message", "修改发生异常: " + e.getMessage());
            log.error("订单修改异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // 订单删除/撤销：同时支持 POST /api/orders/delete 和 DELETE /api/orders
    @PostMapping("/orders/delete")
    public ResponseEntity<Map<String, Object>> deleteOrderPost(@RequestParam Integer orderId) {
        log.info("POST /api/orders/delete called, orderId={}", orderId);
        return handleDelete(orderId);
    }

    @DeleteMapping("/orders")
    public ResponseEntity<Map<String, Object>> deleteOrderDelete(@RequestParam Integer orderId) {
        log.info("DELETE /api/orders called, orderId={}", orderId);
        return handleDelete(orderId);
    }

    private ResponseEntity<Map<String, Object>> handleDelete(Integer orderId) {
        Map<String, Object> body = new HashMap<>();
        try {
            String message = orderService.deleteOrder(orderId);
            body.put("code", 200);
            body.put("message", message);
            log.info("订单删除成功, orderId={}", orderId);
            return ResponseEntity.ok(body);
        } catch (RuntimeException e) {
            body.put("code", 400);
            body.put("message", e.getMessage());
            log.warn("订单删除失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        } catch (Exception e) {
            body.put("code", 500);
            body.put("message", "删除发生异常: " + e.getMessage());
            log.error("订单删除异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}

package com.wms.service;

import com.wms.entity.BigWarehouse;
import com.wms.entity.Order;
import com.wms.mapper.BigWarehouseMapper;
import com.wms.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private BigWarehouseMapper bigWarehouseMapper;

    public List<Order> getOrders(Integer role, Integer workerId) {
        if (role == 0) {
            return orderMapper.findAll();
        } else if (workerId != null && workerId > 0) {
            return orderMapper.findByWorkerIdAndStatusIn(workerId, List.of(1, 2, 3));
        } else {
            return orderMapper.findByStatusIn(List.of(0, 5));
        }
    }

    @Transactional
    public boolean updateOrder(Integer orderId, Double totalPrice, String deadline) {
        int rows = orderMapper.updateOrderPriceAndDeadline(orderId, totalPrice, deadline);
        return rows > 0;
    }

    @Transactional
    public String deleteOrder(Integer orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() != 0 && order.getStatus() != 5) {
            throw new RuntimeException("订单已被接单，不能删除");
        }

        bigWarehouseMapper.restoreQuantity(order.getBigWarehouseId(), order.getQuantity());
        orderMapper.deleteById(orderId);

        return "删除成功，已退回库存";
    }
}

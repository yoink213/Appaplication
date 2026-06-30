package com.wms.service;

import com.wms.entity.Order;
import com.wms.entity.User;
import com.wms.mapper.OrderMapper;
import com.wms.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkerService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Transactional
    public String acceptOrder(Integer orderId, Integer workerId) {
        int activeCount = orderMapper.countActiveByWorkerId(workerId, List.of(1, 2, 3));
        if (activeCount >= 3) {
            throw new RuntimeException("当前已有3单进行中的任务，完成后才可接新单");
        }

        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() != 0 && order.getStatus() != 5) {
            throw new RuntimeException("接单失败，订单已被接取");
        }

        int rows = orderMapper.updateStatusAndWorkerId(orderId, 1, workerId);
        if (rows > 0) {
            return "接单成功，请前往取货";
        }
        throw new RuntimeException("接单失败");
    }

    @Transactional
    public String pickupOrder(Integer orderId, Integer workerId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || order.getWorkerId() == null || !order.getWorkerId().equals(workerId)) {
            throw new RuntimeException("非法操作");
        }

        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态错误");
        }

        int rows = orderMapper.updateStatus(orderId, 2);
        if (rows > 0) {
            return "确认取货成功";
        }
        throw new RuntimeException("操作失败");
    }

    @Transactional
    public String deliverOrder(Integer orderId, Integer workerId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || order.getWorkerId() == null || !order.getWorkerId().equals(workerId)) {
            throw new RuntimeException("非法操作");
        }

        if (order.getStatus() != 2) {
            throw new RuntimeException("订单状态错误");
        }

        int rows = orderMapper.updateStatus(orderId, 3);
        if (rows > 0) {
            return "确认送达成功，等待管理员审核";
        }
        throw new RuntimeException("操作失败");
    }

    @Transactional
    public String auditOrder(Integer orderId, String action) {
        Order order = orderMapper.findById(orderId);
        if (order == null || order.getStatus() != 3) {
            throw new RuntimeException("订单不可审核");
        }

        if ("ACCEPT".equals(action)) {
            orderMapper.updateStatus(orderId, 4);
            userMapper.updateBalance(order.getWorkerId(), order.getTotalPrice().doubleValue());
            return "审核通过，资金已到账";
        } else {
            orderMapper.updateStatusAndWorkerId(orderId, 1, order.getWorkerId());
            return "已驳回，任务已原路返回至对应工人名下";
        }
    }
}

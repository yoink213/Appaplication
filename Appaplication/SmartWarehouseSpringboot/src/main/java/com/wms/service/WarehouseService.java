package com.wms.service;

import com.wms.entity.BigWarehouse;
import com.wms.entity.Order;
import com.wms.mapper.BigWarehouseMapper;
import com.wms.mapper.OrderMapper;
import com.wms.mapper.SequenceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class WarehouseService {

    @Autowired
    private BigWarehouseMapper bigWarehouseMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SequenceMapper sequenceMapper;

    public List<BigWarehouse> getAllItems() {
        return bigWarehouseMapper.findAll();
    }

    public BigWarehouse getItemById(Integer id) {
        return bigWarehouseMapper.findById(id);
    }

    @Transactional
    public String dispatchTask(Integer bigWarehouseId, Integer quantity, Double totalPrice, String deadlineStr, Integer adminId) {
        BigWarehouse item = bigWarehouseMapper.findById(bigWarehouseId);
        if (item == null) {
            throw new RuntimeException("货物不存在");
        }
        if (item.getQuantity() < quantity) {
            throw new RuntimeException("库存不足");
        }

        bigWarehouseMapper.reduceQuantity(bigWarehouseId, quantity);

        Integer orderNoSeq = sequenceMapper.getNextVal("ORDER_NO");
        Integer smallItemSeq = sequenceMapper.getNextVal("SMALL_ITEM");

        sequenceMapper.updateVal("ORDER_NO", orderNoSeq + 1);
        sequenceMapper.updateVal("SMALL_ITEM", smallItemSeq + 1);

        String orderNo = String.format("ORD-%03d", orderNoSeq);
        String smallItemCode = String.format("%03d", smallItemSeq);

        LocalDateTime deadline = LocalDateTime.parse(deadlineStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setSmallItemCode(smallItemCode);
        order.setBigWarehouseId(bigWarehouseId);
        order.setQuantity(quantity);
        order.setTotalPrice(java.math.BigDecimal.valueOf(totalPrice));
        order.setDeadline(deadline);
        order.setStatus(0);
        order.setAdminId(adminId);

        orderMapper.insert(order);

        return orderNo;
    }

    @Transactional
    public BigWarehouse addItem(BigWarehouse item) {
        if (item.getName() == null) {
            throw new RuntimeException("货物名称不能为空");
        }
        if (item.getQuantity() == null) {
            item.setQuantity(0);
        }
        if (item.getPrice() == null) {
            item.setPrice(java.math.BigDecimal.ZERO);
        }
        item.setCode(generateNextCode());
        bigWarehouseMapper.insert(item);
        return bigWarehouseMapper.findById(item.getId());
    }

    public String getNextCode() {
        return generateNextCode();
    }

    private String generateNextCode() {
        String maxCode = bigWarehouseMapper.findMaxCode();
        if (maxCode == null || maxCode.isEmpty()) {
            return "A-001";
        }
        try {
            int dashIdx = maxCode.lastIndexOf('-');
            if (dashIdx < 0) {
                return "A-001";
            }
            String prefix = maxCode.substring(0, dashIdx);
            int num = Integer.parseInt(maxCode.substring(dashIdx + 1));
            return String.format("%s-%03d", prefix, num + 1);
        } catch (Exception e) {
            return "A-001";
        }
    }
}

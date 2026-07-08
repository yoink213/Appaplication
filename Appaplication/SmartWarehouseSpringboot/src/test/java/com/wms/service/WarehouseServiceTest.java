package com.wms.service;

import com.wms.entity.BigWarehouse;
import com.wms.entity.Order;
import com.wms.mapper.BigWarehouseMapper;
import com.wms.mapper.OrderMapper;
import com.wms.mapper.SequenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WarehouseService 测试类（包含黑盒测试与白盒测试）
 *
 * 黑盒测试：关注输入输出，覆盖正常和异常场景
 * 白盒测试：关注内部逻辑分支，如 generateNextCode() 的各种路径
 */
@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private BigWarehouseMapper bigWarehouseMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private SequenceMapper sequenceMapper;

    @InjectMocks
    private WarehouseService warehouseService;

    // -------------------- 测试数据准备 --------------------
    private BigWarehouse sampleItem;
    private final Integer itemId = 1;
    private final Integer quantity = 10;
    private final Double totalPrice = 100.0;
    private final String deadlineStr = "2026-12-31 23:59:59";
    private final Integer adminId = 100;

    @BeforeEach
    void setUp() {
        sampleItem = new BigWarehouse();
        sampleItem.setId(itemId);
        sampleItem.setName("TestItem");
        sampleItem.setQuantity(100);
        sampleItem.setPrice(BigDecimal.valueOf(10.0));
        sampleItem.setCode("A-001");
    }

    // ==================== 黑盒测试 ====================

    @Test
    void testGetAllItems_shouldReturnList() {
        // given
        List<BigWarehouse> mockList = Arrays.asList(sampleItem, new BigWarehouse());
        when(bigWarehouseMapper.findAll()).thenReturn(mockList);

        // when
        List<BigWarehouse> result = warehouseService.getAllItems();

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(bigWarehouseMapper, times(1)).findAll();
    }

    @Test
    void testGetItemById_existingId_shouldReturnItem() {
        when(bigWarehouseMapper.findById(itemId)).thenReturn(sampleItem);

        BigWarehouse result = warehouseService.getItemById(itemId);

        assertNotNull(result);
        assertEquals(itemId, result.getId());
        verify(bigWarehouseMapper, times(1)).findById(itemId);
    }

    @Test
    void testGetItemById_notExisting_shouldReturnNull() {
        when(bigWarehouseMapper.findById(999)).thenReturn(null);

        BigWarehouse result = warehouseService.getItemById(999);

        assertNull(result);
    }

    @Test
    void testDispatchTask_success_shouldReduceStockAndCreateOrder() {
        // given
        when(bigWarehouseMapper.findById(itemId)).thenReturn(sampleItem);
        when(sequenceMapper.getNextVal("ORDER_NO")).thenReturn(1);
        when(sequenceMapper.getNextVal("SMALL_ITEM")).thenReturn(10);

        // when
        String orderNo = warehouseService.dispatchTask(itemId, quantity, totalPrice, deadlineStr, adminId);

        // then
        assertEquals("ORD-001", orderNo);

        // 验证库存减少
        verify(bigWarehouseMapper, times(1)).reduceQuantity(itemId, quantity);

        // 验证序列更新
        verify(sequenceMapper, times(1)).updateVal("ORDER_NO", 2);
        verify(sequenceMapper, times(1)).updateVal("SMALL_ITEM", 11);

        // 验证订单插入
        verify(orderMapper, times(1)).insert(argThat(order ->
                order.getOrderNo().equals("ORD-001") &&
                        order.getSmallItemCode().equals("010") &&
                        order.getBigWarehouseId().equals(itemId) &&
                        order.getQuantity().equals(quantity) &&
                        order.getTotalPrice().equals(BigDecimal.valueOf(totalPrice)) &&
                        order.getDeadline().equals(LocalDateTime.parse(deadlineStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))) &&
                        order.getStatus() == 0 &&
                        order.getAdminId().equals(adminId)
        ));
    }

    @Test
    void testDispatchTask_itemNotFound_shouldThrowException() {
        when(bigWarehouseMapper.findById(itemId)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> warehouseService.dispatchTask(itemId, quantity, totalPrice, deadlineStr, adminId));
        assertEquals("货物不存在", ex.getMessage());

        // 确保后续操作未执行
        verify(bigWarehouseMapper, never()).reduceQuantity(anyInt(), anyInt());
        verify(sequenceMapper, never()).getNextVal(anyString());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void testDispatchTask_insufficientStock_shouldThrowException() {
        // 库存不足：请求数量 > 现有数量
        when(bigWarehouseMapper.findById(itemId)).thenReturn(sampleItem);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> warehouseService.dispatchTask(itemId, 200, totalPrice, deadlineStr, adminId));
        assertEquals("库存不足", ex.getMessage());

        verify(bigWarehouseMapper, never()).reduceQuantity(anyInt(), anyInt());
        verify(sequenceMapper, never()).getNextVal(anyString());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void testAddItem_success_shouldGenerateCodeAndInsert() {
        // given
        BigWarehouse newItem = new BigWarehouse();
        newItem.setName("NewItem");
        newItem.setQuantity(5);
        newItem.setPrice(BigDecimal.valueOf(99.99));

        when(bigWarehouseMapper.findMaxCode()).thenReturn("A-005");
        when(bigWarehouseMapper.insert(any(BigWarehouse.class))).thenAnswer(inv -> {
            BigWarehouse arg = inv.getArgument(0);
            arg.setId(999); // 模拟生成ID
            return null;
        });
        when(bigWarehouseMapper.findById(999)).thenReturn(newItem); // 模拟返回保存后的对象

        // when
        BigWarehouse result = warehouseService.addItem(newItem);

        // then
        assertNotNull(result);
        assertEquals("NewItem", result.getName());
        // 验证代码生成逻辑（因为 insert 后我们返回的是新对象，但实际代码中 insert 后立即 findById，所以需要模拟）
        // 由于我们模拟了 findById，我们可以验证插入时设置的 code
        verify(bigWarehouseMapper, times(1)).insert(argThat(item ->
                item.getCode().equals("A-006")  // A-005 后递增为 A-006
        ));
        verify(bigWarehouseMapper, times(1)).findById(999);
    }

    @Test
    void testAddItem_nameNull_shouldThrowException() {
        BigWarehouse invalidItem = new BigWarehouse();
        invalidItem.setName(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> warehouseService.addItem(invalidItem));
        assertEquals("货物名称不能为空", ex.getMessage());

        verify(bigWarehouseMapper, never()).insert(any());
    }

    @Test
    void testAddItem_nullQuantityAndPrice_shouldSetDefaults() {
        BigWarehouse item = new BigWarehouse();
        item.setName("DefaultItem");
        // quantity 和 price 均为 null

        when(bigWarehouseMapper.findMaxCode()).thenReturn("A-001");
        when(bigWarehouseMapper.insert(any(BigWarehouse.class))).thenAnswer(inv -> {
            BigWarehouse arg = inv.getArgument(0);
            arg.setId(1);
            return null;
        });
        when(bigWarehouseMapper.findById(1)).thenReturn(item);

        BigWarehouse result = warehouseService.addItem(item);

        assertNotNull(result);
        assertEquals(0, result.getQuantity());
        assertEquals(BigDecimal.ZERO, result.getPrice());
    }

    @Test
    void testGetNextCode_shouldReturnNextCode() {
        when(bigWarehouseMapper.findMaxCode()).thenReturn("B-099");

        String nextCode = warehouseService.getNextCode();

        assertEquals("B-100", nextCode);
    }

    // ==================== 白盒测试 ====================
    // 重点测试 generateNextCode() 内部逻辑分支

    @Test
    void testGenerateNextCode_noMaxCode_shouldReturnA001() {
        when(bigWarehouseMapper.findMaxCode()).thenReturn(null);

        // 使用反射调用私有方法 generateNextCode
        String code = invokePrivateGenerateNextCode();

        assertEquals("A-001", code);
    }

    @Test
    void testGenerateNextCode_emptyMaxCode_shouldReturnA001() {
        when(bigWarehouseMapper.findMaxCode()).thenReturn("");

        String code = invokePrivateGenerateNextCode();

        assertEquals("A-001", code);
    }

    @Test
    void testGenerateNextCode_validCode_shouldIncrement() {
        when(bigWarehouseMapper.findMaxCode()).thenReturn("C-123");

        String code = invokePrivateGenerateNextCode();

        assertEquals("C-124", code);
    }

    @Test
    void testGenerateNextCode_invalidFormat_noDash_shouldReturnA001() {
        when(bigWarehouseMapper.findMaxCode()).thenReturn("InvalidCode");

        String code = invokePrivateGenerateNextCode();

        assertEquals("A-001", code); // 捕获异常后返回 A-001
    }

    @Test
    void testGenerateNextCode_invalidNumber_shouldReturnA001() {
        when(bigWarehouseMapper.findMaxCode()).thenReturn("D-abc");

        String code = invokePrivateGenerateNextCode();

        assertEquals("A-001", code);
    }

    @Test
    void testGenerateNextCode_prefixWithMultipleDashes_shouldUseLastDash() {
        // 假设代码格式为 "A-B-001"，应取最后一个 '-' 后的数字
        when(bigWarehouseMapper.findMaxCode()).thenReturn("A-B-001");

        String code = invokePrivateGenerateNextCode();

        assertEquals("A-B-002", code);
    }

    // 辅助方法：通过反射调用私有 generateNextCode
    private String invokePrivateGenerateNextCode() {
        try {
            java.lang.reflect.Method method = WarehouseService.class.getDeclaredMethod("generateNextCode");
            method.setAccessible(true);
            return (String) method.invoke(warehouseService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 白盒补充：测试 dispatchTask 中序列更新的事务性（实际事务由Spring管理，单元测试可忽略，但可以验证调用顺序）
    @Test
    void testDispatchTask_sequenceUpdateInvoked() {
        when(bigWarehouseMapper.findById(itemId)).thenReturn(sampleItem);
        when(sequenceMapper.getNextVal("ORDER_NO")).thenReturn(5);
        when(sequenceMapper.getNextVal("SMALL_ITEM")).thenReturn(20);

        warehouseService.dispatchTask(itemId, quantity, totalPrice, deadlineStr, adminId);

        // 验证顺序：先get后update
        verify(sequenceMapper).getNextVal("ORDER_NO");
        verify(sequenceMapper).updateVal("ORDER_NO", 6);
        verify(sequenceMapper).getNextVal("SMALL_ITEM");
        verify(sequenceMapper).updateVal("SMALL_ITEM", 21);
        // 验证先 reduce 再 insert（但实际顺序不重要，因为事务保证）
        // 但我们可验证调用顺序，使用 InOrder
        var inOrder = inOrder(bigWarehouseMapper, sequenceMapper, orderMapper);
        inOrder.verify(bigWarehouseMapper).reduceQuantity(itemId, quantity);
        inOrder.verify(sequenceMapper).getNextVal("ORDER_NO");
        inOrder.verify(sequenceMapper).updateVal("ORDER_NO", 6);
        inOrder.verify(sequenceMapper).updateVal("SMALL_ITEM", 21);
        inOrder.verify(orderMapper).insert(any(Order.class));
    }
}
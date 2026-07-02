package com.wms.mapper;

import com.wms.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    List<Order> findAll();

    List<Order> findByWorkerIdAndStatusIn(@Param("workerId") Integer workerId, @Param("statuses") List<Integer> statuses);

    List<Order> findByStatusIn(@Param("statuses") List<Integer> statuses);

    Order findById(@Param("id") Integer id);

    int insert(Order order);

    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    int updateStatusAndWorkerId(@Param("id") Integer id, @Param("status") Integer status, @Param("workerId") Integer workerId);

    int updateOrderPriceAndDeadline(@Param("id") Integer id, @Param("totalPrice") Double totalPrice, @Param("deadline") String deadline);

    int deleteById(@Param("id") Integer id);

    int countActiveByWorkerId(@Param("workerId") Integer workerId, @Param("statuses") List<Integer> statuses);
}

package com.wms.mapper;

import com.wms.entity.BigWarehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BigWarehouseMapper {

    List<BigWarehouse> findAll();

    BigWarehouse findById(@Param("id") Integer id);

    int reduceQuantity(@Param("id") Integer id, @Param("quantity") Integer quantity);

    int restoreQuantity(@Param("id") Integer id, @Param("quantity") Integer quantity);

    int insert(BigWarehouse item);

    String findMaxCode();
}

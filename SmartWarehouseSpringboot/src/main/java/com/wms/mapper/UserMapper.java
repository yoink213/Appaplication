package com.wms.mapper;

import com.wms.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);

    User findById(@Param("id") Integer id);

    int updateBalance(@Param("userId") Integer userId, @Param("amount") Double amount);
}

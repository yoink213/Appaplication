package com.wms.service;

import com.wms.entity.User;
import com.wms.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    @Autowired
    private UserMapper userMapper;

    public User login(String username, String password) {
        return userMapper.findByUsernameAndPassword(username, password);
    }
}

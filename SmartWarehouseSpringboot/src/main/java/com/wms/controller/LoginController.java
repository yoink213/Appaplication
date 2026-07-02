package com.wms.controller;

import com.wms.entity.LoginRequest;
import com.wms.entity.User;
import com.wms.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        User user = loginService.login(request.getUsername(), request.getPassword());
        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("role", user.getRole());
            data.put("realName", user.getRealName());
            data.put("balance", user.getBalance());
            response.put("code", 200);
            response.put("message", "登录成功");
            response.put("data", data);
        } else {
            response.put("code", 401);
            response.put("message", "用户名或密码错误，或账号已被禁用");
        }
        return response;
    }
}

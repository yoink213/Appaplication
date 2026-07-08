package com.wms.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private Integer role;      // 0-管理员 1-工人
    private String realName;
    private Double balance;
    private Integer status;    // 0-停用 1-启用
    private LocalDateTime createdAt;
}

package com.jjdx.bookmeeting.model.dto.admin.user;

import lombok.Data;

import java.io.Serializable;

/**
 用户创建请求
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     账号
     */
    private String userAccount;

    /**
     用户昵称
     */
    private String userName;

    /**
     用户角色：user/admin/ban
     */
    private String role;

    /**
     邮箱
     */
    private String email;

    /**
     手机号
     */
    private String phone;

    private static final long serialVersionUID = 1L;
}

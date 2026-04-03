package com.jjdx.bookmeeting.model.dto.user.user;

import lombok.Data;

import java.io.Serializable;

/**
 用户更新个人信息请求
 */
@Data
public class UserUpdateMyRequest implements Serializable {

    /**
     用户昵称
     */
    private String userName;

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

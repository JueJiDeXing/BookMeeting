package com.jjdx.bookmeeting.model.dto.admin;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户重置密码请求
 */
@Data
public class UserResetPasswordRequest implements Serializable {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 新密码（如果不传则使用默认密码123456）
     */
    private String password;

    private static final long serialVersionUID = 1L;
}

package com.jjdx.bookmeeting.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新密码请求体
 */
@Data
public class UserUpdatePasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 原密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}

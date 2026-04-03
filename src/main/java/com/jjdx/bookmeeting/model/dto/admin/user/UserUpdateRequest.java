package com.jjdx.bookmeeting.model.dto.admin.user;

import lombok.Data;

import java.io.Serializable;

/**
 用户更新请求
 */
@Data
public class UserUpdateRequest implements Serializable {

    /**
     id
     */
    private Long id;

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

    /**
     用户头像
     */
    private String avatar;

    /**
     用户角色：user/admin/ban (管理员可修改)
     */
    private String role;

    private static final long serialVersionUID = 1L;
}

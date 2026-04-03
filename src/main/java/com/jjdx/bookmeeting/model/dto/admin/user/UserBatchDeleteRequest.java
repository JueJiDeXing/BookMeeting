package com.jjdx.bookmeeting.model.dto.admin.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 用户批量删除请求
 */
@Data
public class UserBatchDeleteRequest implements Serializable {

    /**
     用户ID列表
     */
    private List<Long> ids;

    private static final long serialVersionUID = 1L;
}

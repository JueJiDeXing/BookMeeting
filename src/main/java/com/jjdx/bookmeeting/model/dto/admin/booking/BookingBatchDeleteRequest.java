package com.jjdx.bookmeeting.model.dto.admin.booking;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 预定批量删除请求
 */
@Data
public class BookingBatchDeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 预定记录ID列表
     */
    private List<Long> ids;
}

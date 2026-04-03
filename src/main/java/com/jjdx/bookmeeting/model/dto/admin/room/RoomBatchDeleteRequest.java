package com.jjdx.bookmeeting.model.dto.admin.room;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 会议室批量删除请求
 */
@Data
public class RoomBatchDeleteRequest implements Serializable {

    /**
     会议室ID列表
     */
    private List<Long> ids;

    private static final long serialVersionUID = 1L;
}

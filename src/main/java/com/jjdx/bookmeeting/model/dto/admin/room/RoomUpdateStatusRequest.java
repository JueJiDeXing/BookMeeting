// RoomUpdateStatusRequest.java
package com.jjdx.bookmeeting.model.dto.admin.room;

import lombok.Data;

import java.io.Serializable;

/**
 * 会议室更新状态请求
 */
@Data
public class RoomUpdateStatusRequest implements Serializable {

    /**
     * 会议室ID
     */
    private Long id;

    /**
     * 状态（0-可用 1-维护中 2-被占用）
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}

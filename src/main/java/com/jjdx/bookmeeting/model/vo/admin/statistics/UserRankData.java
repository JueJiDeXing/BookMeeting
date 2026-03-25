package com.jjdx.bookmeeting.model.vo.admin.statistics;

import lombok.Data;

import java.util.*;

// UserRankData.java
@Data
public class UserRankData {
    /**
     用户ID
     */
    private Long userId;
    /**
     用户账号
     */
    private String userAccount;
    /**
     用户名
     */
    private String userName;
    /**
     预定次数
     */
    private Integer bookingCount;
}

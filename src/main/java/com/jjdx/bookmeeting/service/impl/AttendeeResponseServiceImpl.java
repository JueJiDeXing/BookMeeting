package com.jjdx.bookmeeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.mapper.AttendeeResponseMapper;
import com.jjdx.bookmeeting.model.entity.AttendeeResponse;
import com.jjdx.bookmeeting.model.enums.AttendeeResponseStatusEnum;
import com.jjdx.bookmeeting.service.AttendeeResponseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 参会人员响应服务实现
 */
@Service
@Slf4j
public class AttendeeResponseServiceImpl extends ServiceImpl<AttendeeResponseMapper, AttendeeResponse> implements AttendeeResponseService {

    @Override
    public List<AttendeeResponse> getByBookingId(Long bookingId) {
        LambdaQueryWrapper<AttendeeResponse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendeeResponse::getBookingId, bookingId)
                .orderByAsc(AttendeeResponse::getStatus)
                .orderByDesc(AttendeeResponse::getResponseTime);
        return list(wrapper);
    }

    @Override
    public List<AttendeeResponse> getByUserId(Long userId) {
        LambdaQueryWrapper<AttendeeResponse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendeeResponse::getUserId, userId)
                .orderByDesc(AttendeeResponse::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long bookingId, Long userId, Integer status, String remark) {
        // 检查参数
        if (status == null || !AttendeeResponseStatusEnum.isValidEnum(status)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值无效");
        }

        // 查找响应记录
        LambdaQueryWrapper<AttendeeResponse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendeeResponse::getBookingId, bookingId)
                .eq(AttendeeResponse::getUserId, userId);

        AttendeeResponse response = getOne(wrapper);
        if (response == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "参会人员响应记录不存在");
        }

        // 更新状态
        response.setStatus(status);
        response.setResponseTime(LocalDateTime.now());
        response.setRemark(remark);

        boolean updated = updateById(response);
        if (updated) {
            log.info("用户[{}]响应预定[{}]，状态：{}，备注：{}", userId, bookingId, status, remark);
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchCreate(Long bookingId, List<Long> attendeeIds) {
        if (CollectionUtils.isEmpty(attendeeIds)) {
            return true;
        }

        // 先删除原有记录
        deleteByBookingId(bookingId);

        // 批量创建新记录
        List<AttendeeResponse> responseList = attendeeIds.stream()
                .map(attendeeId -> {
                    AttendeeResponse response = new AttendeeResponse();
                    response.setBookingId(bookingId);
                    response.setUserId(attendeeId);
                    response.setStatus(AttendeeResponseStatusEnum.PENDING.getValue()); // 默认待确认
                    return response;
                })
                .collect(Collectors.toList());

        boolean saved = saveBatch(responseList);
        if (saved) {
            log.info("为预定[{}]批量创建{}个参会人员响应", bookingId, attendeeIds.size());
        }

        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByBookingId(Long bookingId) {
        LambdaQueryWrapper<AttendeeResponse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendeeResponse::getBookingId, bookingId);
        return remove(wrapper);
    }

    @Override
    public int countConfirmedByBookingId(Long bookingId) {
        LambdaQueryWrapper<AttendeeResponse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendeeResponse::getBookingId, bookingId)
                .eq(AttendeeResponse::getStatus, AttendeeResponseStatusEnum.CONFIRMED.getValue()); // 已确认
        return (int) count(wrapper);
    }

    @Override
    public boolean hasResponded(Long bookingId, Long userId) {
        LambdaQueryWrapper<AttendeeResponse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendeeResponse::getBookingId, bookingId)
                .eq(AttendeeResponse::getUserId, userId)
                .ne(AttendeeResponse::getStatus, AttendeeResponseStatusEnum.PENDING.getValue()); // 不是待确认状态
        return count(wrapper) > 0;
    }

    @Override
    public List<Long> getUnrespondedUserIds(Long bookingId) {
        LambdaQueryWrapper<AttendeeResponse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendeeResponse::getBookingId, bookingId)
                .eq(AttendeeResponse::getStatus, AttendeeResponseStatusEnum.PENDING.getValue()); // 待确认

        List<AttendeeResponse> unrespondedList = list(wrapper);
        return unrespondedList.stream()
                .map(AttendeeResponse::getUserId)
                .collect(Collectors.toList());
    }
}

package com.jjdx.bookmeeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.DeleteRequest;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.constant.UserConstant;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.interceptor.aop.annotation.AuthCheck;
import com.jjdx.bookmeeting.model.dto.admin.user.*;
import com.jjdx.bookmeeting.model.entity.User;
import com.jjdx.bookmeeting.model.vo.UserVO;
import com.jjdx.bookmeeting.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 用户接口
 */
@RestController
@RequestMapping("/admin/user")
@Slf4j
public class AdminUserController {

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 校验账号是否已存在
        if (userService.getByUserAccount(userAddRequest.getUserAccount()) != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        User user = userService.addUser(userAddRequest);
        return ResultUtils.success(user.getId());
    }

    /**
     删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 不能删除自己
        User currentUser = userService.getLoginUser(request);
        if (currentUser.getId().equals(deleteRequest.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能删除当前登录账号");
        }

        boolean removed = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(removed);
    }

    /**
     批量删除用户
     */
    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchDeleteUser(@RequestBody UserBatchDeleteRequest batchDeleteRequest,
                                                 HttpServletRequest request) {
        if (batchDeleteRequest == null || batchDeleteRequest.getIds() == null || batchDeleteRequest.getIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 不能删除自己
        User currentUser = userService.getLoginUser(request);
        if (batchDeleteRequest.getIds().contains(currentUser.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能删除当前登录账号");
        }

        boolean removed = userService.removeByIds(batchDeleteRequest.getIds());
        return ResultUtils.success(removed);
    }

    /**
     更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
                                            HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查用户是否存在
        User oldUser = userService.getById(userUpdateRequest.getId());
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);

        boolean updated = userService.updateById(user);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新用户失败");
        }
        return ResultUtils.success(true);
    }

    /**
     重置密码
     */
    @PostMapping("/resetPassword")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> resetPassword(@RequestBody UserResetPasswordRequest resetPasswordRequest,
                                               HttpServletRequest request) {
        if (resetPasswordRequest == null || resetPasswordRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User user = userService.getById(resetPasswordRequest.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        boolean updated = userService.resetPassword(resetPasswordRequest, user);

        return ResultUtils.success(updated);
    }

    /**
     根据 id 获取用户
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 脱敏
        user.setPassword(null);
        return ResultUtils.success(user);
    }

    /**
     根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     分页获取用户列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                   HttpServletRequest request) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();

        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));

        // 脱敏
        userPage.getRecords().forEach(user -> user.setPassword(null));

        return ResultUtils.success(userPage);
    }

    /**
     分页获取用户封装列表
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                       HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();

        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));

        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVOList);

        return ResultUtils.success(userVOPage);
    }

    // endregion
}

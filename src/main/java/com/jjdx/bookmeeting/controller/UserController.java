package com.jjdx.bookmeeting.controller;

import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.model.dto.user.UserLoginRequest;
import com.jjdx.bookmeeting.model.dto.user.UserRegisterRequest;
import com.jjdx.bookmeeting.model.dto.user.UserUpdateMyRequest;
import com.jjdx.bookmeeting.model.dto.user.UserUpdatePasswordRequest;
import com.jjdx.bookmeeting.model.entity.User;
import com.jjdx.bookmeeting.model.vo.LoginUserVO;
import com.jjdx.bookmeeting.service.UserService;
import com.jjdx.bookmeeting.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 用户接口
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    // region 注册登录

    /**
     用户注册

     @param userRegisterRequest
     @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     用户登录

     @param userLoginRequest
     @param request
     @return
     */
    @PostMapping("/login")
    public BaseResponse<String> userLogin(@RequestBody UserLoginRequest userLoginRequest,
                                          HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String password = userLoginRequest.getPassword();
        if (StringUtils.isAnyBlank(userAccount, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLogin(userAccount, password, request);

        HashMap<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(loginUserVO.getId()));
        map.put("userName", loginUserVO.getUserName());
        map.put("role", loginUserVO.getRole());
        String token = JwtUtils.generateToken(map);
        return ResultUtils.success(token);
    }

    // endregion

    /**
     用户注销

     @param request
     @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     获取当前登录用户

     @param request
     @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }
    // region 更新信息

    /**
     用户更新密码

     @param userUpdatePasswordRequest
     @param request
     @return
     */
    @PostMapping("/update/password")
    public BaseResponse<Boolean> updatePassword(@RequestBody UserUpdatePasswordRequest userUpdatePasswordRequest,
                                                HttpServletRequest request) {
        // 1. 校验请求参数
        if (userUpdatePasswordRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String oldPassword = userUpdatePasswordRequest.getOldPassword();
        String newPassword = userUpdatePasswordRequest.getNewPassword();
        String checkPassword = userUpdatePasswordRequest.getCheckPassword();

        if (StringUtils.isAnyBlank(oldPassword, newPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 校验新密码和确认密码是否一致
        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 3. 校验新密码长度
        if (newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能少于8位");
        }

        // 4. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 5. 调用service更新密码
        boolean result = userService.updatePassword(loginUser.getId(), oldPassword, newPassword, checkPassword);

        return ResultUtils.success(result);
    }

    /**
     更新个人信息

     @param userUpdateMyRequest
     @param request
     @return
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
                                              HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 校验数据
        if (StringUtils.isNotBlank(userUpdateMyRequest.getUserName())
                && userUpdateMyRequest.getUserName().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名称不能超过50个字符");
        }

        // 更新用户信息
        boolean result = userService.updateUserInfo(loginUser.getId(), userUpdateMyRequest);
        return ResultUtils.success(result);
    }

    // endregion


}

package com.jjdx.bookmeeting.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jjdx.bookmeeting.model.dto.admin.user.UserAddRequest;
import com.jjdx.bookmeeting.model.dto.admin.user.UserQueryRequest;
import com.jjdx.bookmeeting.model.dto.admin.user.UserResetPasswordRequest;
import com.jjdx.bookmeeting.model.dto.user.user.UserUpdateMyRequest;
import com.jjdx.bookmeeting.model.entity.User;
import com.jjdx.bookmeeting.model.vo.LoginUserVO;
import com.jjdx.bookmeeting.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 用户服务
 */
public interface UserService extends IService<User> {

    /**
     用户注册
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     用户登录
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     获取当前登录用户（可能为null）
     */
    User getLoginUserNullable(HttpServletRequest request);

    /**
     获取当前登录用户（不为null）
     */
    User getLoginUser(HttpServletRequest request);

    /**
     是否为管理员
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     是否为管理员
     */
    boolean isAdmin(User user);

    /**
     用户注销
     */
    boolean userLogout(HttpServletRequest request);

    /**
     获取脱敏的已登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     获取脱敏的用户信息
     */
    UserVO getUserVO(User user);

    /**
     获取脱敏的用户信息列表
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     获取查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     根据账号获取用户
     */
    default User getByUserAccount(String userAccount) {
        return lambdaQuery().eq(User::getUserAccount, userAccount).one();
    }

    /**
     用户更新密码

     @param userId        用户ID
     @param oldPassword   原密码
     @param newPassword   新密码
     @param checkPassword 确认密码
     @return 是否成功
     */
    boolean updatePassword(Long userId, String oldPassword, String newPassword, String checkPassword);

    /**
     更新用户信息

     @param userId
     @param userUpdateMyRequest
     @return
     */
    boolean updateUserInfo(Long userId, UserUpdateMyRequest userUpdateMyRequest);

    /**
     添加用户
     */
    User addUser(UserAddRequest userAddRequest);

    /**
     重设密码

     @param resetPasswordRequest
     @param user
     @return
     */
    boolean resetPassword(UserResetPasswordRequest resetPasswordRequest, User user);
}

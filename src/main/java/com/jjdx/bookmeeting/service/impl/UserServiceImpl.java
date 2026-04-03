package com.jjdx.bookmeeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.constant.CommonConstant;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.mapper.UserMapper;
import com.jjdx.bookmeeting.model.dto.admin.user.UserAddRequest;
import com.jjdx.bookmeeting.model.dto.admin.user.UserQueryRequest;
import com.jjdx.bookmeeting.model.dto.admin.user.UserResetPasswordRequest;
import com.jjdx.bookmeeting.model.dto.user.user.UserUpdateMyRequest;
import com.jjdx.bookmeeting.model.entity.User;
import com.jjdx.bookmeeting.model.enums.UserRoleEnum;
import com.jjdx.bookmeeting.model.vo.LoginUserVO;
import com.jjdx.bookmeeting.model.vo.UserVO;
import com.jjdx.bookmeeting.service.UserService;
import com.jjdx.bookmeeting.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 用户服务实现
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     盐值，混淆密码
     */
    public static final String SALT = "jjdx";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_account", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex(
                    (SALT + userPassword).getBytes(StandardCharsets.UTF_8)
            );
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setPassword(encryptPassword);
            // 默认角色为用户
            user.setRole(UserRoleEnum.USER.getValue());
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userAccount);
        queryWrapper.eq("password", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        return this.getLoginUserVO(user);
    }


    /**
     获取当前登录用户

     @param request
     @return 未登录则返回null
     */
    @Override
    public User getLoginUserNullable(HttpServletRequest request) {
        // 先判断是否已登录
        Object userId = request.getAttribute("id");
        if (userId == null) {
            return null;
        }
        return this.getById(userId.toString());
    }

    /**
     获取当前登录用户

     @param request
     @return 未登录则报错
     */
    @org.jetbrains.annotations.NotNull
    @Override
    public @NotNull User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userId = request.getAttribute("id");
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        User currentUser = this.getById(userId.toString());
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public User addUser(UserAddRequest userAddRequest) {
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);

        // 设置默认密码（123456）
        String defaultPassword = "123456";
        String encryptPassword = DigestUtils.md5DigestAsHex(
                (SALT + defaultPassword).getBytes(StandardCharsets.UTF_8)
        );
        user.setPassword(encryptPassword);

        // 设置默认用户名（如果没传）
        if (user.getUserName() == null || user.getUserName().isEmpty()) {
            user.setUserName(user.getUserAccount());
        }

        // 设置默认角色（如果没传）
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole(UserRoleEnum.USER.getValue());
        }

        boolean saved = save(user);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加用户失败");
        }
        return user;
    }

    @Override
    public boolean resetPassword(UserResetPasswordRequest resetPasswordRequest, User user) {
        // 设置新密码
        String newPassword = resetPasswordRequest.getPassword();
        if (newPassword == null || newPassword.isEmpty()) {
            newPassword = "123456"; // 默认密码
        }

        String encryptPassword = DigestUtils.md5DigestAsHex(
                (SALT + newPassword).getBytes(StandardCharsets.UTF_8)
        );

        user.setPassword(encryptPassword);
        return updateById(user);
    }

    /**
     是否为管理员

     @param request
     @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        User user = getLoginUser(request);
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getRole());
    }

    /**
     用户注销

     @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // TODO
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) return null;
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) return null;
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public boolean updateUserInfo(Long userId, UserUpdateMyRequest userUpdateMyRequest) {
        // 1. 校验用户是否存在
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 2. 校验邮箱是否已被其他用户使用
        if (StringUtils.isNotBlank(userUpdateMyRequest.getEmail())) {
            QueryWrapper<User> emailQuery = new QueryWrapper<>();
            emailQuery.eq("email", userUpdateMyRequest.getEmail())
                    .ne("id", userId);
            Long emailCount = this.baseMapper.selectCount(emailQuery);
            if (emailCount > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被其他用户使用");
            }
        }

        // 3. 校验手机号是否已被其他用户使用
        if (StringUtils.isNotBlank(userUpdateMyRequest.getPhone())) {
            QueryWrapper<User> phoneQuery = new QueryWrapper<>();
            phoneQuery.eq("phone", userUpdateMyRequest.getPhone())
                    .ne("id", userId);
            Long phoneCount = this.baseMapper.selectCount(phoneQuery);
            if (phoneCount > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号已被其他用户使用");
            }
        }

        // 4. 更新数据
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setUserName(userUpdateMyRequest.getUserName());
        updateUser.setEmail(userUpdateMyRequest.getEmail());
        updateUser.setPhone(userUpdateMyRequest.getPhone());

        boolean result = this.updateById(updateUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }

        return true;
    }

    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword, String checkPassword) {
        // 1. 校验参数
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID错误");
        }
        if (StringUtils.isAnyBlank(oldPassword, newPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }

        // 2. 校验新密码和确认密码是否一致
        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 3. 校验新密码长度（不少于8位）
        if (newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能少于8位");
        }

        // 4. 查询用户是否存在
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 5. 验证原密码是否正确
        String encryptedOldPassword = DigestUtils.md5DigestAsHex((SALT + oldPassword).getBytes());
        if (!user.getPassword().equals(encryptedOldPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码错误");
        }

        // 6. 加密新密码并更新
        String encryptedNewPassword = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes());
        user.setPassword(encryptedNewPassword);

        return this.updateById(user);
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userRole = userQueryRequest.getRole();
        String email = userQueryRequest.getEmail();
        String phone = userQueryRequest.getPhone();
        Integer isDelete = userQueryRequest.getIsDelete();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(userAccount), "user_account", userAccount);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "role", userRole);
        queryWrapper.eq(isDelete != null, "is_delete", isDelete);
        queryWrapper.eq(StringUtils.isNotBlank(email), "email", email);
        queryWrapper.eq(StringUtils.isNotBlank(phone), "phone", phone);
        queryWrapper.like(StringUtils.isNotBlank(userName), "user_name", userName);

        // 排序
        if (SqlUtils.validSortField(sortField)) {
            queryWrapper.orderBy(true, sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        } else {
            // 默认按创建时间倒序
            queryWrapper.orderByDesc("create_time");
        }

        return queryWrapper;
    }

}

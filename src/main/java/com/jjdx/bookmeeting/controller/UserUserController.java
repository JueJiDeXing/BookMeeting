package com.jjdx.bookmeeting.controller;

import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.model.entity.User;
import com.jjdx.bookmeeting.model.vo.UserVO;
import com.jjdx.bookmeeting.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端-用户接口
 */
@RestController
@RequestMapping("/user/user")
@Slf4j
public class UserUserController {

    @Resource
    private UserService userService;

    /**
     * 获取所有可用用户列表（用于选择参会人员）
     */
    @GetMapping("/list/all")
    public BaseResponse<List<UserVO>> listAllUsers(HttpServletRequest request) {
        // 获取当前登录用户（确保已登录）
        userService.getLoginUser(request);

        List<User> userList = userService.lambdaQuery()
                .eq(User::getIsDelete, 0)
                .ne(User::getRole, "ban") // 排除禁用账号
                .orderByAsc(User::getUserName)
                .list();

        List<UserVO> userVOList = userList.stream()
                .map(user -> {
                    UserVO vo = new UserVO();
                    vo.setId(user.getId());
                    vo.setUserAccount(user.getUserAccount());
                    vo.setUserName(user.getUserName());
                    vo.setEmail(user.getEmail());
                    vo.setPhone(user.getPhone());
                    return vo;
                })
                .collect(Collectors.toList());

        return ResultUtils.success(userVOList);
    }
}

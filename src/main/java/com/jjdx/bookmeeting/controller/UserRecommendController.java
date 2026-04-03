package com.jjdx.bookmeeting.controller;

import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.model.dto.user.recommend.RecommendRequest;
import com.jjdx.bookmeeting.model.vo.recommend.RecommendRoomVO;
import com.jjdx.bookmeeting.service.RecommendService;
import com.jjdx.bookmeeting.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 用户端-智能推荐接口
 */
@RestController
@RequestMapping("/user/recommend")
@Slf4j
public class UserRecommendController {

    @Resource
    private RecommendService recommendService;

    @Resource
    private UserService userService;

    /**
     智能推荐会议室
     */
    @PostMapping("/rooms")
    public BaseResponse<List<RecommendRoomVO>> recommendRooms(@RequestBody RecommendRequest request,
                                                              HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = userService.getLoginUser(httpRequest).getId();
        List<RecommendRoomVO> recommendRooms = recommendService.recommendRooms(userId, request);

        return ResultUtils.success(recommendRooms);
    }

    /**
     获取热门会议室
     */
    @GetMapping("/hot")
    public BaseResponse<List<RecommendRoomVO>> getHotRooms(@RequestParam(defaultValue = "5") int limit) {
        List<RecommendRoomVO> hotRooms = recommendService.getHotRooms(limit);
        return ResultUtils.success(hotRooms);
    }

    /**
     获取相似会议室
     */
    @GetMapping("/similar/{roomId}")
    public BaseResponse<List<RecommendRoomVO>> getSimilarRooms(@PathVariable Long roomId,
                                                               @RequestParam(defaultValue = "5") int limit) {
        if (roomId == null || roomId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<RecommendRoomVO> similarRooms = recommendService.getSimilarRooms(roomId, limit);
        return ResultUtils.success(similarRooms);
    }
}

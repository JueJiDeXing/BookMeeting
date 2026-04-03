package com.jjdx.bookmeeting.service.impl;

import com.jjdx.bookmeeting.model.dto.admin.statistics.StatisticsRequest;
import com.jjdx.bookmeeting.model.entity.BookingRecord;
import com.jjdx.bookmeeting.model.entity.MeetingRoom;
import com.jjdx.bookmeeting.model.entity.User;
import com.jjdx.bookmeeting.model.enums.BookingStatusEnum;
import com.jjdx.bookmeeting.model.vo.admin.statistics.*;
import com.jjdx.bookmeeting.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 统计服务实现
 */
@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    @Resource
    private BookingRecordService bookingRecordService;

    @Resource
    private MeetingRoomService meetingRoomService;

    @Resource
    private UserService userService;

    @Resource
    private AttendeeResponseService attendeeResponseService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public StatisticsVO getStatistics(StatisticsRequest request) {
        // 1. 获取时间范围
        TimeRange timeRange = getTimeRange(request);

        // 2. 查询预定记录
        List<BookingRecord> bookings = bookingRecordService.lambdaQuery()
                .ge(BookingRecord::getStartTime, timeRange.getStartTime())
                .le(BookingRecord::getEndTime, timeRange.getEndTime())
                .in(BookingRecord::getStatus,
                        BookingStatusEnum.PENDING.getValue(),
                        BookingStatusEnum.IN_PROGRESS.getValue(),
                        BookingStatusEnum.COMPLETED.getValue()) // 有效预定（待签到、进行中、已完成）
                .list();

        // 3. 构建统计结果
        StatisticsVO statistics = new StatisticsVO();

        // 3.1 总览数据
        statistics.setOverview(calculateOverview(bookings, timeRange));

        // 3.2 会议室使用排行
        statistics.setRoomRank(calculateRoomRank(bookings));

        // 3.3 时间段热度分布
        statistics.setTimeHeat(calculateTimeHeat(bookings));

        // 3.4 每日预定趋势
        statistics.setDailyTrend(calculateDailyTrend(bookings, timeRange));

        // 3.5 预定人排行
        statistics.setUserRank(calculateUserRank(bookings));

        // 3.6 会议室使用详情
        statistics.setRoomDetails(calculateRoomDetails(bookings, timeRange));

        return statistics;
    }

    @Override
    public String exportStatistics(StatisticsRequest request) {
        StatisticsVO stats = getStatistics(request);

        StringBuilder csv = new StringBuilder();

        // 总览数据
        csv.append("=== 总览数据 ===\n");
        csv.append("总预定次数,").append(stats.getOverview().getTotalBookings()).append("\n");
        csv.append("总会议时长(小时),").append(stats.getOverview().getTotalHours()).append("\n");
        csv.append("平均每场时长(小时),").append(stats.getOverview().getAvgHoursPerBooking()).append("\n");
        csv.append("参会总人次,").append(stats.getOverview().getTotalAttendees()).append("\n");
        csv.append("平均参会人数,").append(stats.getOverview().getAvgAttendeesPerBooking()).append("\n");
        csv.append("会议室使用率(%),").append(stats.getOverview().getUtilizationRate()).append("\n");
        csv.append("高峰期,").append(stats.getOverview().getPeakHours()).append("\n\n");

        // 会议室使用排行
        csv.append("=== 会议室使用排行 ===\n");
        csv.append("会议室名称,预定次数\n");
        for (RoomRankData data : stats.getRoomRank()) {
            csv.append(data.getRoomName()).append(",").append(data.getBookingCount()).append("\n");
        }
        csv.append("\n");

        // 时间段热度分布
        csv.append("=== 时间段热度分布 ===\n");
        csv.append("时间段,预定次数\n");
        for (TimeHeatData data : stats.getTimeHeat()) {
            csv.append(data.getTimeSlot()).append(",").append(data.getCount()).append("\n");
        }
        csv.append("\n");

        // 每日预定趋势
        csv.append("=== 每日预定趋势 ===\n");
        csv.append("日期,预定次数,总时长(小时)\n");
        for (TrendData data : stats.getDailyTrend()) {
            csv.append(data.getDate()).append(",")
                    .append(data.getCount()).append(",")
                    .append(data.getTotalHours()).append("\n");
        }
        csv.append("\n");

        // 预定人排行
        csv.append("=== 预定人排行 ===\n");
        csv.append("用户名,账号,预定次数\n");
        for (UserRankData data : stats.getUserRank()) {
            csv.append(data.getUserName()).append(",")
                    .append(data.getUserAccount()).append(",")
                    .append(data.getBookingCount()).append("\n");
        }

        return csv.toString();
    }

    /**
     获取时间范围
     */
    private TimeRange getTimeRange(StatisticsRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime, endTime = now;

        if (request.getType() != null) {
            switch (request.getType()) {
                case "week":
                    startTime = now.minusWeeks(1);
                    break;
                case "month":
                    startTime = now.minusMonths(1);
                    break;
                case "quarter":
                    startTime = now.minusMonths(3);
                    break;
                case "year":
                    startTime = now.minusYears(1);
                    break;
                case "custom":
                    startTime = request.getStartTime();
                    endTime = request.getEndTime();
                    break;
                default: // day
                    startTime = now.minusDays(1);
                    break;
            }
        } else {
            startTime = now.minusMonths(1);
        }

        return new TimeRange(startTime, endTime);
    }

    /**
     计算总览数据
     */
    private OverviewData calculateOverview(List<BookingRecord> bookings, TimeRange timeRange) {
        OverviewData overview = new OverviewData();

        // 总预定次数
        int totalBookings = bookings.size();
        overview.setTotalBookings(totalBookings);

        // 总会议时长（分钟转小时）
        double totalMinutes = bookings.stream()
                .mapToDouble(b -> java.time.Duration.between(b.getStartTime(), b.getEndTime()).toMinutes())
                .sum();
        double totalHours = totalMinutes / 60;
        overview.setTotalHours(Math.round(totalHours * 10) / 10.0);

        // 平均每场时长
        if (totalBookings > 0) {
            double avgHours = totalHours / totalBookings;
            overview.setAvgHoursPerBooking(Math.round(avgHours * 10) / 10.0);
        } else {
            overview.setAvgHoursPerBooking(0.0);
        }

        // 参会总人次
        int totalAttendees = bookings.stream()
                .mapToInt(b -> {
                    String attendeesId = b.getAttendeesId();
                    if (attendeesId == null) return 1;
                    return attendeesId.split(",").length;
                })
                .sum();
        overview.setTotalAttendees(totalAttendees);

        // 平均每场参会人数
        if (totalBookings > 0) {
            overview.setAvgAttendeesPerBooking(Math.round((double) totalAttendees / totalBookings * 10) / 10.0);
        } else {
            overview.setAvgAttendeesPerBooking(0.0);
        }

        // 会议室使用率（假设工作时间8:00-20:00，12小时/天）
        int totalDays = (int) java.time.Duration.between(timeRange.getStartTime(), timeRange.getEndTime()).toDays() + 1;
        List<MeetingRoom> rooms = meetingRoomService.lambdaQuery().eq(MeetingRoom::getIsDelete, 0).list();
        int totalRooms = rooms.size();

        if (totalRooms > 0 && totalDays > 0) {
            // 理论可用时长 = 会议室数量 × 天数 × 12小时
            double theoreticalHours = totalRooms * totalDays * 12.0;
            double utilizationRate = theoreticalHours > 0 ? (totalHours / theoreticalHours) * 100 : 0;
            overview.setUtilizationRate((double) Math.round(utilizationRate));
        } else {
            overview.setUtilizationRate(0.0);
        }

        // 计算高峰期
        overview.setPeakHours(calculatePeakHours(bookings));

        // 计算趋势（对比上一期）
        LocalDateTime lastPeriodStart = timeRange.getStartTime().minus(java.time.Duration.between(
                timeRange.getStartTime(), timeRange.getEndTime()));
        LocalDateTime lastPeriodEnd = timeRange.getStartTime();

        long lastPeriodBookings = bookingRecordService.lambdaQuery()
                .ge(BookingRecord::getStartTime, lastPeriodStart)
                .le(BookingRecord::getEndTime, lastPeriodEnd)
                .in(BookingRecord::getStatus,
                        BookingStatusEnum.PENDING.getValue(),
                        BookingStatusEnum.IN_PROGRESS.getValue(),
                        BookingStatusEnum.COMPLETED.getValue())
                .count();

        if (lastPeriodBookings > 0) {
            double trend = ((double) totalBookings - lastPeriodBookings) / lastPeriodBookings * 100;
            overview.setBookingTrend(Math.round(trend * 10) / 10.0);
        } else {
            overview.setBookingTrend(totalBookings > 0 ? 100.0 : 0.0);
        }

        return overview;
    }

    /**
     计算高峰期
     */
    private String calculatePeakHours(List<BookingRecord> bookings) {
        // 统计每个小时段的预定次数
        Map<String, Integer> hourCount = new HashMap<>();

        for (BookingRecord booking : bookings) {
            int startHour = booking.getStartTime().getHour();
            String hourSlot = String.format("%02d:00-%02d:00", startHour, startHour + 1);
            hourCount.put(hourSlot, hourCount.getOrDefault(hourSlot, 0) + 1);
        }

        // 找出预定次数最多的时段
        Optional<Map.Entry<String, Integer>> maxEntry = hourCount.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (maxEntry.isPresent()) {
            return maxEntry.get().getKey();
        }
        return "暂无数据";
    }

    /**
     计算会议室使用排行
     */
    private List<RoomRankData> calculateRoomRank(List<BookingRecord> bookings) {
        // 统计每个会议室的预定次数
        Map<Long, Integer> roomCount = new HashMap<>();
        for (BookingRecord booking : bookings) {
            roomCount.put(booking.getRoomId(), roomCount.getOrDefault(booking.getRoomId(), 0) + 1);
        }

        // 获取会议室名称
        List<RoomRankData> rankList = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : roomCount.entrySet()) {
            MeetingRoom room = meetingRoomService.getById(entry.getKey());
            if (room != null) {
                RoomRankData data = new RoomRankData();
                data.setRoomId(entry.getKey());
                data.setRoomName(room.getRoomName());
                data.setBookingCount(entry.getValue());
                rankList.add(data);
            }
        }

        // 按预定次数降序排序
        rankList.sort((a, b) -> b.getBookingCount().compareTo(a.getBookingCount()));

        return rankList;
    }

    /**
     计算时间段热度分布
     */
    private List<TimeHeatData> calculateTimeHeat(List<BookingRecord> bookings) {
        // 统计每个小时段的预定次数（8:00-20:00）
        Map<String, Integer> heatMap = new LinkedHashMap<>();

        // 初始化所有时段
        for (int hour = 8; hour < 20; hour++) {
            String timeSlot = String.format("%02d:00-%02d:00", hour, hour + 1);
            heatMap.put(timeSlot, 0);
        }

        // 统计
        for (BookingRecord booking : bookings) {
            int startHour = booking.getStartTime().getHour();
            if (startHour >= 8 && startHour < 20) {
                String timeSlot = String.format("%02d:00-%02d:00", startHour, startHour + 1);
                heatMap.put(timeSlot, heatMap.getOrDefault(timeSlot, 0) + 1);
            }
        }

        // 转换为列表
        List<TimeHeatData> heatList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : heatMap.entrySet()) {
            TimeHeatData data = new TimeHeatData();
            data.setTimeSlot(entry.getKey());
            data.setCount(entry.getValue());
            heatList.add(data);
        }

        return heatList;
    }

    /**
     计算每日预定趋势
     */
    private List<TrendData> calculateDailyTrend(List<BookingRecord> bookings, TimeRange timeRange) {
        // 按日期分组
        Map<String, List<BookingRecord>> dailyMap = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getStartTime().format(DATE_FORMATTER)));

        // 生成时间范围内的所有日期
        List<String> allDates = new ArrayList<>();
        LocalDateTime current = timeRange.getStartTime();
        while (current.isBefore(timeRange.getEndTime())) {
            allDates.add(current.format(DATE_FORMATTER));
            current = current.plusDays(1);
        }

        // 构建趋势数据
        List<TrendData> trendList = new ArrayList<>();
        for (String date : allDates) {
            List<BookingRecord> dayBookings = dailyMap.getOrDefault(date, new ArrayList<>());

            double dayMinutes = dayBookings.stream()
                    .mapToDouble(b -> java.time.Duration.between(b.getStartTime(), b.getEndTime()).toMinutes())
                    .sum();

            TrendData data = new TrendData();
            data.setDate(date);
            data.setCount(dayBookings.size());
            data.setTotalHours(Math.round(dayMinutes / 60 * 10) / 10.0);
            trendList.add(data);
        }

        return trendList;
    }

    /**
     计算预定人排行
     */
    private List<UserRankData> calculateUserRank(List<BookingRecord> bookings) {
        // 统计每个用户的预定次数
        Map<Long, Integer> userCount = new HashMap<>();
        for (BookingRecord booking : bookings) {
            userCount.put(booking.getUserId(), userCount.getOrDefault(booking.getUserId(), 0) + 1);
        }

        // 获取用户信息
        List<UserRankData> rankList = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : userCount.entrySet()) {
            User user = userService.getById(entry.getKey());
            if (user != null) {
                UserRankData data = new UserRankData();
                data.setUserId(entry.getKey());
                data.setUserAccount(user.getUserAccount());
                data.setUserName(user.getUserName());
                data.setBookingCount(entry.getValue());
                rankList.add(data);
            }
        }

        // 按预定次数降序排序
        rankList.sort((a, b) -> b.getBookingCount().compareTo(a.getBookingCount()));

        return rankList;
    }

    /**
     计算会议室使用详情
     */
    private List<RoomDetailData> calculateRoomDetails(List<BookingRecord> bookings, TimeRange timeRange) {
        // 获取所有会议室
        List<MeetingRoom> rooms = meetingRoomService.lambdaQuery().eq(MeetingRoom::getIsDelete, 0).list();

        // 统计每个会议室的预定次数和时长
        Map<Long, Integer> bookingCountMap = new HashMap<>();
        Map<Long, Double> totalMinutesMap = new HashMap<>();

        for (BookingRecord booking : bookings) {
            Long roomId = booking.getRoomId();
            bookingCountMap.put(roomId, bookingCountMap.getOrDefault(roomId, 0) + 1);

            double minutes = java.time.Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
            totalMinutesMap.put(roomId, totalMinutesMap.getOrDefault(roomId, 0.0) + minutes);
        }

        // 计算使用率
        int totalDays = (int) java.time.Duration.between(timeRange.getStartTime(), timeRange.getEndTime()).toDays() + 1;
        double workingHoursPerDay = 12.0; // 8:00-20:00

        List<RoomDetailData> details = new ArrayList<>();
        for (MeetingRoom room : rooms) {
            Long roomId = room.getId();
            int bookingCount = bookingCountMap.getOrDefault(roomId, 0);
            double totalMinutes = totalMinutesMap.getOrDefault(roomId, 0.0);
            double totalHours = totalMinutes / 60;

            // 使用率 = 实际使用时长 / (天数 × 12小时) × 100%
            double theoreticalHours = totalDays * workingHoursPerDay;
            double utilizationRate = theoreticalHours > 0 ? (totalHours / theoreticalHours) * 100 : 0;

            // 计算该会议室的最常用时间段
            String peakTime = calculateRoomPeakTime(roomId, bookings);

            RoomDetailData data = new RoomDetailData();
            data.setRoomId(roomId);
            data.setRoomName(room.getRoomName());
            data.setBookingCount(bookingCount);
            data.setTotalHours(Math.round(totalHours * 10) / 10.0);
            data.setUtilizationRate((double) Math.round(utilizationRate));
            data.setPeakTime(peakTime);
            details.add(data);
        }

        // 按预定次数降序排序
        details.sort((a, b) -> b.getBookingCount().compareTo(a.getBookingCount()));

        return details;
    }

    /**
     计算单个会议室的最常用时间段
     */
    private String calculateRoomPeakTime(Long roomId, List<BookingRecord> bookings) {
        Map<String, Integer> hourCount = new HashMap<>();

        for (BookingRecord booking : bookings) {
            if (booking.getRoomId().equals(roomId)) {
                int startHour = booking.getStartTime().getHour();
                String hourSlot = String.format("%02d:00-%02d:00", startHour, startHour + 1);
                hourCount.put(hourSlot, hourCount.getOrDefault(hourSlot, 0) + 1);
            }
        }

        Optional<Map.Entry<String, Integer>> maxEntry = hourCount.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        return maxEntry.map(Map.Entry::getKey).orElse("-");
    }

    /**
     时间范围内部类
     */
    private static class TimeRange {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        public TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }
    }
}

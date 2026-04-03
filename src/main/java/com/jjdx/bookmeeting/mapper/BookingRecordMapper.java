package com.jjdx.bookmeeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jjdx.bookmeeting.model.entity.BookingRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookingRecordMapper extends BaseMapper<BookingRecord> {
}

create table attendee_response
(
    id            bigint auto_increment comment 'ID'
        primary key,
    booking_id    bigint                             not null comment '预定记录ID',
    user_id       bigint                             not null comment '参会人ID',
    status        int      default 0                 null comment '响应状态（0-待确认 1-已确认 2-已拒绝）',
    response_time datetime                           null comment '响应时间',
    remark        varchar(1000)                      null comment '备注（如拒绝原因）',
    is_delete     int      default 0                 null comment '是否从会议中删除（0-未删除 1-删除）',
    create_time   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_booking_user
        unique (booking_id, user_id)
)
    comment '参会人员响应预定关系表';

create index idx_status
    on attendee_response (status);

create index idx_user
    on attendee_response (user_id);

create table booking_record
(
    id            bigint auto_increment comment '预定记录ID'
        primary key,
    user_id       bigint                             not null comment '预定人ID',
    attendees_id  text                               not null comment '参会人员ID，含预定人，逗号分隔',
    room_id       bigint                             not null comment '会议室ID',
    title         varchar(100)                       not null comment '会议标题',
    description   varchar(1000)                      null comment '会议描述',
    start_time    datetime                           not null comment '会议开始时间',
    end_time      datetime                           not null comment '会议结束时间',
    remind_before int      default 15                null comment '提前提醒分钟数',
    status        int      default 0                 null comment '会议状态（0-待签到 1-进行中 2-已完成 3-已取消 4-未签到超时）',
    actual_start  datetime                           null comment '实际开始时间（签到时间）',
    actual_end    datetime                           null comment '实际结束时间（释放时间）',
    create_time   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '用户预定会议室关系表';

create index idx_room_id
    on booking_record (room_id);

create index idx_status_time
    on booking_record (status, start_time);

create index idx_time_range
    on booking_record (start_time, end_time);

create index idx_user_id
    on booking_record (user_id);

create table equipment
(
    id             bigint auto_increment comment '设备ID'
        primary key,
    equipment_name varchar(50)                        not null comment '设备名称，如：白板、投影仪',
    equipment_code varchar(50)                        not null comment '设备代码',
    status         int      default 1                 null comment '状态（0-正常 1-不可用）',
    is_delete      int      default 0                 null comment '0-未删除 1-已删除',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    category_id    bigint                             null comment '分类ID'
)
    comment '设备表';

create table equipment_category
(
    id            bigint auto_increment comment '分类ID'
        primary key,
    category_name varchar(50)                        not null comment '分类名称，如：白板、投影、音频',
    sort_order    int      default 0                 null comment '排序',
    create_time   datetime default CURRENT_TIMESTAMP null
)
    comment '设备分类表';

create table meeting_room
(
    id            bigint auto_increment comment '会议室ID'
        primary key,
    room_name     varchar(100)                       not null comment '会议室名称',
    building      varchar(100)                       not null comment '楼栋',
    floor         int                                not null comment '楼层',
    room_number   varchar(100)                       not null comment '房间号，如：101、2号会议室',
    location_desc varchar(200) as (concat(`building`, _utf8mb4' ', `floor`, _utf8mb4'æ¥¼ ',
                                          `room_number`)) stored comment '完整位置描述，如：A栋 3楼 301',
    capacity      int                                not null comment '可容纳人数',
    description   text                               null comment '会议室描述',
    status        int      default 0                 null comment '0-可用 1-维护中 2-被占用',
    is_delete     int      default 0                 null comment '0-未删除 1-已删除',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '会议室表';

create table message
(
    id             bigint auto_increment comment '消息ID'
        primary key,
    user_id        bigint                             not null comment '接收人ID',
    booking_id     bigint                             null comment '关联的预定ID',
    title          varchar(100)                       not null comment '消息标题',
    content        varchar(1000)                      not null comment '消息内容',
    type           int      default 0                 null comment '消息类型：0-会议提醒 1-系统通知',
    status         int      default 0                 null comment '状态：0-未读 1-已读',
    remind_task_id bigint                             null comment '关联的提醒任务ID',
    create_time    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    read_time      datetime                           null comment '阅读时间'
)
    comment '站内消息表';

create index idx_user_id
    on message (user_id);

create index idx_user_status
    on message (user_id, status);

create table remind_task
(
    id          bigint auto_increment comment '提醒任务ID'
        primary key,
    booking_id  bigint                             not null comment '预定记录ID',
    user_id     bigint                             not null comment '参会人ID',
    remind_time datetime                           not null comment '提醒时间',
    remind_type int      default 0                 null comment '提醒方式（0-站内信 1-邮件 2-全部）',
    status      int      default 0                 null comment '状态（0-待发送 1-已发送 2-发送失败 3-已取消）',
    retry_count int      default 0                 null comment '重试次数',
    error_msg   varchar(500)                       null comment '错误信息',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '会议提醒任务表';

create index idx_booking_id
    on remind_task (booking_id);

create index idx_remind_time_status
    on remind_task (remind_time, status);

create index idx_status_retry
    on remind_task (status, retry_count);

create table room_equipment
(
    id           bigint auto_increment
        primary key,
    room_id      bigint                             not null comment '会议室ID',
    equipment_id bigint                             not null comment '设备ID',
    is_available int      default 1                 null comment '设备是否可用（0-不可用 1-正常可用）',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_room_equipment
        unique (room_id, equipment_id)
)
    comment '会议室含有设备关系表';

create table user
(
    id           bigint auto_increment comment '用户ID'
        primary key,
    user_account varchar(100)                           not null comment '账号',
    user_name    varchar(50)                            null comment '昵称',
    password     varchar(300)                           not null comment '密码（加密后）',
    email        varchar(100)                           null comment '邮箱',
    phone        varchar(100)                           null comment '手机号',
    role         varchar(100) default 'user'            not null comment '角色（user/admin/ban）',
    is_delete    int          default 0                 null comment '0-未删除 1-已删除',
    create_time  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint email
        unique (email),
    constraint phone
        unique (phone),
    constraint username
        unique (user_account)
)
    comment '用户表';

create table user_preference
(
    id                      bigint auto_increment comment 'ID'
        primary key,
    user_id                 bigint                             not null comment '用户ID',
    favorite_room_ids       text                               null comment '常用会议室ID列表(JSON)',
    preferred_equipment_ids text                               null comment '常用设备ID列表(JSON)',
    preferred_time_slots    text                               null comment '常用时间段(JSON)',
    favorite_building       varchar(100)                       null comment '常用楼栋',
    avg_attendee_count      int      default 0                 null comment '平均参会人数',
    update_time             datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_user_id
        unique (user_id)
)
    comment '用户偏好表';


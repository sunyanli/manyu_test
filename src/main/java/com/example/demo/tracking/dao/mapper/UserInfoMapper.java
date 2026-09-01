package com.example.demo.tracking.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.tracking.model.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信息 Mapper
 *
 * @author AiWork
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {
}
package com.example.org.repository;

import com.example.org.model.entity.TransferRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransferRecordRepository extends BaseMapper<TransferRecord> {
}
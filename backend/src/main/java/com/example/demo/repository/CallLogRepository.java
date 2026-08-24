package com.example.demo.repository;

import com.example.demo.model.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CallLogRepository extends JpaRepository<CallLog, Long> {

    // 按人员类型分组统计
    @Query("SELECT c.personType AS label, COUNT(c) AS value FROM CallLog c " +
           "WHERE c.callTime BETWEEN :startTime AND :endTime " +
           "GROUP BY c.personType ORDER BY value DESC")
    List<Object[]> countByPersonType(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    // 按人员层级分组统计
    @Query("SELECT c.personLevel AS label, COUNT(c) AS value FROM CallLog c " +
           "WHERE c.callTime BETWEEN :startTime AND :endTime " +
           "GROUP BY c.personLevel ORDER BY value DESC")
    List<Object[]> countByPersonLevel(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    // 按部门分组统计
    @Query("SELECT c.department AS label, COUNT(c) AS value FROM CallLog c " +
           "WHERE c.callTime BETWEEN :startTime AND :endTime " +
           "GROUP BY c.department ORDER BY value DESC")
    List<Object[]> countByDepartment(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    // 按时间趋势统计（按天聚合）
    @Query("SELECT FUNCTION('DATE', c.callTime) AS label, COUNT(c) AS value FROM CallLog c " +
           "WHERE c.callTime BETWEEN :startTime AND :endTime " +
           "GROUP BY FUNCTION('DATE', c.callTime) ORDER BY label ASC")
    List<Object[]> countByTimeTrend(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);
}
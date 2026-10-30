package com.example.algorithm.mapper;

import java.util.Map;
import java.util.Set;

/**
 * SQL provider for AlgorithmCallRecordMapper with whitelist validation.
 */
public class AlgorithmCallRecordSqlProvider {

    private static final Set<String> ALLOWED_DIMENSIONS = Set.of(
            "caller_type", "caller_level", "caller_dept", "algorithm_type"
    );

    private static final Set<String> ALLOWED_FORMATS = Set.of(
            "%Y-%m-%d", "%Y-%u", "%Y-%m"
    );

    public String selectStatisticsByDimension(Map<String, Object> params) {
        String dimension = (String) params.get("dimension");
        String validated = validateDimension(dimension);
        return "SELECT " + validated + " AS dimension_key, COUNT(*) AS count "
                + "FROM algorithm_call_record "
                + "WHERE gmt_create >= #{startTime} AND gmt_create <= #{endTime} "
                + "GROUP BY " + validated + " "
                + "LIMIT 10000";
    }

    public String selectTrend(Map<String, Object> params) {
        String format = (String) params.get("format");
        String validatedFormat = validateFormat(format);
        return "SELECT DATE_FORMAT(gmt_create, '" + validatedFormat + "') AS time_label, COUNT(*) AS count "
                + "FROM algorithm_call_record "
                + "WHERE gmt_create >= #{startTime} AND gmt_create <= #{endTime} "
                + "AND (#{algorithmType} IS NULL OR algorithm_type = #{algorithmType}) "
                + "GROUP BY DATE_FORMAT(gmt_create, '" + validatedFormat + "') "
                + "ORDER BY time_label "
                + "LIMIT 10000";
    }

    /**
     * Validate dimension against whitelist.
     *
     * @param dimension the dimension to validate
     * @return the validated dimension
     * @throws IllegalArgumentException if dimension is not allowed
     */
    public static String validateDimension(String dimension) {
        if (dimension == null || !ALLOWED_DIMENSIONS.contains(dimension)) {
            throw new IllegalArgumentException("Invalid dimension: " + dimension);
        }
        return dimension;
    }

    /**
     * Validate time format against whitelist.
     *
     * @param format the format to validate
     * @return the validated format
     * @throws IllegalArgumentException if format is not allowed
     */
    public static String validateFormat(String format) {
        if (format == null || !ALLOWED_FORMATS.contains(format)) {
            throw new IllegalArgumentException("Invalid format: " + format);
        }
        return format;
    }
}

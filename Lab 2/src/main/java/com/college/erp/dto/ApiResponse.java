package com.college.erp.dto;

/**
 * Consistent response envelope for every ERP endpoint.
 */
public record ApiResponse<T>(boolean success, T data, String error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> failed(String error) {
        return new ApiResponse<>(false, null, error);
    }
}

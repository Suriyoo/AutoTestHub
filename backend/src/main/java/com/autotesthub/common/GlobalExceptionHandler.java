
package com.autotesthub.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex) {

        String message = ex.getMessage();

        // 打印完整异常堆栈，方便开发阶段排查问题
        log.error("业务运行异常：", ex);

        // 401：未登录或认证失败
        if ("用户名或密码错误".equals(message)
                || "请先登录".equals(message)
                || "Token无效或已过期".equals(message)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(401, message, null));
        }

        // 403：用户无权限
        if ("用户已被禁用".equals(message)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(403, message, null));
        }

        // 404：资源不存在
        if ("项目不存在".equals(message)
                || "用例不存在".equals(message)
                || "环境不存在".equals(message)
                || "执行记录不存在".equals(message)
                || "场景不存在".equals(message)
                || "场景步骤不存在".equals(message)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, message, null));
        }

        // 400：请求参数、业务规则或 JSON 格式错误
        if (message != null && (
                "不允许修改用例所属项目".equals(message)
                || "不允许修改环境所属项目".equals(message)
                || "不允许修改场景所属项目".equals(message)
                || "步骤排序参数错误".equals(message)
                || message.contains("不是合法的JSON格式")
                || message.contains("必须是JSON对象")
                || message.contains("环境变量不是合法的JSON格式")
                || message.contains("环境变量必须是JSON对象")
                || "环境变量JSON格式错误".equals(message)
        )) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(400, message, null));
        }

        // 500：其他未分类业务异常
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "服务器内部错误", null));
    }

    // DTO 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {

        log.error("请求参数校验失败：", ex);

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("请求参数不合法");

        return ResponseEntity
                .badRequest()
                .body(new ApiResponse<>(400, message, null));
    }

    // 请求参数约束异常
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintException(
            ConstraintViolationException ex) {

        log.error("请求参数约束异常：", ex);

        return ResponseEntity
                .badRequest()
                .body(new ApiResponse<>(400, ex.getMessage(), null));
    }

    // 其他系统异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOtherException(
            Exception ex) {

        log.error("未处理的系统异常：", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "服务器内部错误", null));
    }
}

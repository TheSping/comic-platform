package comic.platform.backend.core.exception;

import comic.platform.backend.core.RestBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 专门拦截ComicException
     */
    @ExceptionHandler(ComicException.class)
    public RestBean<Void> handleBusinessException(ComicException e) {
        log.warn("业务拦截: {}", e.getMessage());
        return RestBean.failure(e.getCode(), e.getMessage());
    }

    /**
     * 处理 @RequestBody 实体类参数校验失败 (MethodArgumentNotValidException)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RestBean<Void> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // 提取所有失败字段的错误提示信息并拼接
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("实体参数校验失败: {}", errorMessage);
        return RestBean.failure(400, "参数校验失败: " + errorMessage);
    }

    /**
     * 拦截所有 Exception 类型的异常
     */
    @ExceptionHandler(Exception.class)
    public RestBean<Void> handleException(Exception e) {
        log.error("系统发生未捕获的异常: ", e);
        return RestBean.failure(500, "服务器开小差了: " + e.getMessage());
    }
}

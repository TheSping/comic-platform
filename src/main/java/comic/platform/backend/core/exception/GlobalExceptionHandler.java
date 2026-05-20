package comic.platform.backend.core.exception;

import comic.platform.backend.entity.RestBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
     * 拦截所有 Exception 类型的异常
     */
    @ExceptionHandler(Exception.class)
    public RestBean<Void> handleException(Exception e) {
        log.error("系统发生未捕获的异常: ", e);
        return RestBean.failure(500, "服务器开小差了: " + e.getMessage());
    }
}

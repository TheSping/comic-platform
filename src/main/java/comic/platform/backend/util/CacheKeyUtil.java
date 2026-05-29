package comic.platform.backend.util;

import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 缓存 Key 生成工具类
 * 交给 Spring 管理，方便在 SpEL 中调用
 */
@Component("cacheKeyUtil") // 明确指定 Bean 的名称
public class CacheKeyUtil {

    /**
     * 对字符串进行 MD5 加密
     */
    public String md5(String text) {
        if (text == null) return "";
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }
}

package comic.platform.backend.util;

import lombok.SneakyThrows;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * URL 处理通用工具类
 */
public class UrlUtils {

    /**
     * 处理相对路径，拼成绝对路径，解决双斜杠/少斜杠问题
     */
    public static String resolveUrl(String baseUrl, String path) {
        if (path == null || path.isEmpty()) return baseUrl;
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path; // 已经是绝对路径，直接返回
        }

        // 核心修复：完美处理斜杠拼接
        boolean baseEndsWithSlash = baseUrl.endsWith("/");
        boolean pathStartsWithSlash = path.startsWith("/");

        if (baseEndsWithSlash && pathStartsWithSlash) {
            // 都有斜杠，截掉 path 的第一个斜杠
            return baseUrl + path.substring(1);
        } else if (!baseEndsWithSlash && !pathStartsWithSlash) {
            // 都没有斜杠，中间补一个
            return baseUrl + "/" + path;
        }

        // 刚好一个有一个没有，直接完美拼接
        return baseUrl + path;
    }

    /**
     * 将用户输入的关键词进行URL编码处理，塞进{{key}}
     */
    @SneakyThrows
    public static String renderUrl(String template, String keyword) {
        if (template == null || !template.contains("{{key}}")) {
            return template;
        }
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.name());
        return template.replace("{{key}}", encodedKeyword);
    }
}

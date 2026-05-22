package comic.platform.backend.controller;

import comic.platform.backend.service.NetworkService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@RestController
@RequestMapping("/api/proxy")
public class ProxyController {

    private final NetworkService networkService;

    // 只有一个构造函数时，Spring 会自动在这里进行注入
    // 注入 NetworkService 替代 OkHttpClient
    public ProxyController(NetworkService networkService) {
        this.networkService = networkService;
    }

    /**
     * 图片流式代理接口
     * 前端调用示例： <img src="/api/proxy/image?url=图片的绝对地址">
     */
    @GetMapping("/image")
    public void proxyImage(@RequestParam("url") String targetUrl, HttpServletResponse response) {
        // 调用 Service 获取字节流
        byte[] imageBytes = networkService.getImageBytes(targetUrl);

        if (imageBytes != null) {
            try {
                // 由于 byte[] 不带原服务器的 Header，我们通过 URL 推断 Content-Type
                response.setContentType(guessContentType(targetUrl));

                // 设置缓存头（缓存7天）
                response.setHeader("Cache-Control", "max-age=604800");

                // 将内存中的 byte[] 直接写入响应输出流
                OutputStream out = response.getOutputStream();
                StreamUtils.copy(imageBytes, out);
                out.flush();

            } catch (Exception e) {
                log.error("代理图片输出流异常，目标URL: {}", targetUrl, e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            // 获取不到 byte[]，说明 Service 侧网络请求失败
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * 根据 URL 后缀简单推断 Content-Type，默认返回 image/jpeg
     */
    private String guessContentType(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".png")) return "image/png";
        if (lowerUrl.endsWith(".gif")) return "image/gif";
        if (lowerUrl.endsWith(".webp")) return "image/webp";
        return "image/jpeg"; // 兜底
    }
}
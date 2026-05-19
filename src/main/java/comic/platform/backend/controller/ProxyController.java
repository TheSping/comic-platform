package comic.platform.backend.controller;

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
@RequiredArgsConstructor
public class ProxyController {

    private final OkHttpClient okHttpClient;

    /**
     * 图片流式代理接口
     * 前端调用示例： <img src="/api/proxy/image?url=图片的绝对地址">
     */
    @GetMapping("/image")
    public void proxyImage(@RequestParam("url") String targetUrl, HttpServletResponse response) {
        // 动态提取 Referer (防盗链的核心)
        // 很多网站只允许自己域名下的网页请求图片，所以我们把它的域名扒下来伪装成 Referer
        String referer = getBaseUrl(targetUrl);

        // 构造带有“伪装面具”的 HTTP 请求
        Request request = new Request.Builder()
                .url(targetUrl) // 目标网站
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36") // 伪装Chrome浏览器
                .header("Referer", referer) //伪装网站自己的域名
                .build(); // 构建

        // 发起请求并开启“流式传输”通道
        try (Response okHttpResponse = okHttpClient.newCall(request).execute()) {

            if (okHttpResponse.isSuccessful() && okHttpResponse.body() != null) {
                // 告诉前端，将要发过去的是一张图片 (继承目标网站的 Content-Type，jpg兜底)
                response.setContentType(okHttpResponse.header("Content-Type", "image/jpeg"));

                // 设置缓存头，让浏览器的内存帮你存图片，减轻服务器压力（缓存7天）
                response.setHeader("Cache-Control", "max-age=604800");

                // 接通水管：一头连接目标网站的输入流，一头连接我们 Vue 前端的输出流
                InputStream in = okHttpResponse.body().byteStream();
                OutputStream out = response.getOutputStream();

                // 使用 Spring 自带的工具类，把数据流直接传到Web
                StreamUtils.copy(in, out);
                out.flush();

            } else {
                log.warn("代理图片失败，HTTP状态码: {}", okHttpResponse.code());
                response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 返回 404
            }

        } catch (Exception e) {
            log.error("代理图片发生异常，目标URL: {}", targetUrl, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 返回 500
        }
    }

    /**
     * 工具方法：从完整 URL 中提取出根域名作为 Referer
     * 比如 https://img.domain.com/abc/1.jpg -> 提取为 https://img.domain.com/
     */
    private String getBaseUrl(String fullUrl) {
        try {
            URI uri = new URI(fullUrl);
            return uri.getScheme() + "://" + uri.getHost() + "/";
        } catch (URISyntaxException e) {
            return fullUrl; // 解析失败就原样返回
        }
    }
}
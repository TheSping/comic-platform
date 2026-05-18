package comic.platform.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkService {

    // Spring 会自动把配置好的单例 OkHttpClient 注入进来
    private final OkHttpClient okHttpClient;

    /**
     * 发起 GET 请求抓取网页源码或接口 JSON
     */
    public String getHtml(String url) {
        // 构造请求
        Request request = new Request.Builder()
                .url(url)
                // 伪造浏览器指纹，防止被直接识别为 Java 爬虫
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build();

        // 发起请求并获取响应
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            } else {
                log.error("抓取失败，HTTP状态码: {}", response.code());
            }
        } catch (IOException e) {
            log.error("网络请求异常，目标地址: {}", url, e);
        }

        return ""; // 发生异常时容错返回空字符串
    }
}

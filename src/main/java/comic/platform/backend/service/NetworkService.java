package comic.platform.backend.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Service
public class NetworkService {

    private final OkHttpClient okHttpClient;

    // 统一管理 User-Agent，后续改这里即可
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // 只有一个构造函数时，Spring 会自动在这里进行注入
    public NetworkService(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    /**
     * 发起 GET 请求抓取网页内容
     */
    public String getHtml(String url) {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            } else {
                log.error("抓取失败，HTTP状态码: {}", response.code());
            }
        } catch (IOException e) {
            log.error("网络请求异常，目标地址: {}", url, e);
        }

        return "";
    }

    /**
     * 发起 GET 请求，返回图片的二进制数据
     */
    public byte[] getImageBytes(String url) {
        // 构建防盗链 Referer
        String referer = getBaseUrl(url);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Referer", referer)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                // 注意：这里会将整张图片读取为字节数组
                return response.body().bytes();
            } else {
                log.warn("获取图片失败，HTTP状态码: {}", response.code());
            }
        } catch (IOException e) {
            log.error("网络请求图片异常，目标地址: {}", url, e);
        }

        return null;
    }

    /**
     * 工具方法：从完整 URL 中提取出根域名作为 Referer
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

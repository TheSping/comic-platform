package comic.platform.backend.core.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                // 设置连接超时时间（防止被死链卡死线程）
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                // 配置连接池：最大 50 个闲置连接，存活 5 分钟
                .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
                // 未来我们突破防盗链的拦截器，也会加在这里
                .build();
    }
}

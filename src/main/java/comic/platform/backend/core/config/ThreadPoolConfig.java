package comic.platform.backend.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

    @Bean("crawlerExecutor")
    public ExecutorService crawlerExecutor() {
        // 配置 20 个专属爬虫线程。如果你部署的服务器性能好，甚至可以调到 50
        return Executors.newFixedThreadPool(20);
    }
}

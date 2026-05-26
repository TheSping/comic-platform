package comic.platform.backend.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
@EnableAsync
@Configuration
public class ThreadPoolConfig {

    // 20 个专属爬虫线程
    @Bean("crawlerExecutor")
    public ExecutorService crawlerExecutor() {
        return Executors.newFixedThreadPool(20);
    }

    // 异步任务线程池
    @Bean("asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        // 队列可以设大一点，因为任务执行极快，积压风险小
        executor.setQueueCapacity(500);
        // 拒绝策略：如果连快车道都满了，由调用者所在的线程（也就是主线程）自己去执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("async-task-");
        executor.initialize();
        return executor;
    }
}

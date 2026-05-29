package comic.platform.backend.core.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 1. 配置 Jackson 序列化工具，解决反序列化时强制转换为 LinkedHashMap 的问题
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // 关键：激活默认类型校验，保存具体类的信息到 JSON 中，确保 List<Map<String,String>> 结构正确还原
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 2. 默认缓存配置（全局兜底配置）
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 默认 10 分钟过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer));

        // 3. 针对不同业务前缀进行分层 TTL 配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 搜索结果缓存：5 分钟
        cacheConfigurations.put("search", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        // 翻页结果缓存：5 分钟
        cacheConfigurations.put("search_page", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        // 章节目录缓存：30 分钟
        cacheConfigurations.put("toc", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        // 内容图片缓存：2 小时
        cacheConfigurations.put("content", defaultCacheConfig.entryTtl(Duration.ofHours(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
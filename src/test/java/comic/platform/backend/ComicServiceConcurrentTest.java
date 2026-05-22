package comic.platform.backend;

import comic.platform.backend.entity.ComicSource;
import comic.platform.backend.module.comic.ComicService;
import comic.platform.backend.module.comic.ComicSourceService;
import comic.platform.backend.module.comic.parser.ParserEngine;
import comic.platform.backend.service.NetworkService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
public class ComicServiceConcurrentTest {

    @Resource
    private ComicService comicService;

    // 🌟 核心：使用 @MockBean 把底层的依赖全部“变成傀儡”，由我们来操控它们的行为
    @MockBean
    private ComicSourceService comicSourceService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private ParserEngine parserEngine;


    @Test
    @DisplayName("硬核并发测试：验证时间压缩与单节点容错")
    void testConcurrentSearch() throws Exception {
        // ================= 1. 伪造战场环境 =================

        // 伪造 3 个已启用的书源
        List<ComicSource> mockSources = new ArrayList<>();
        mockSources.add(createMockSource(1, "闪电漫画", "https://fast.com"));
        mockSources.add(createMockSource(2, "乌龟漫画", "https://slow.com"));
        mockSources.add(createMockSource(3, "崩溃漫画", "https://boom.com"));

        // 告诉傀儡 sourceService：当有人调 getActiveSources 时，把这 3 个假书源给他
        when(comicSourceService.getAllActiveSourcesFromCache()).thenReturn(mockSources);

        // ================= 2. 编排网络延迟与崩溃 =================

        // 闪电漫画：睡 500 毫秒就返回
        when(networkService.getHtml(ArgumentMatchers.contains("fast.com"))).thenAnswer(invocation -> {
            Thread.sleep(500);
            return "<html>闪电漫画的内容</html>";
        });

        // 乌龟漫画：睡 2000 毫秒才返回
        when(networkService.getHtml(ArgumentMatchers.contains("slow.com"))).thenAnswer(invocation -> {
            Thread.sleep(2000);
            return "<html>乌龟漫画的内容</html>";
        });

        // 崩溃漫画：直接抛出异常，模拟网站挂了或防爬虫拦截
        when(networkService.getHtml(ArgumentMatchers.contains("boom.com"))).thenThrow(new RuntimeException("网站拒绝访问！"));

        // ================= 3. 编排解析引擎 =================

        // 告诉傀儡解析器：只要收到闪电漫画的 HTML，就吐出 2 本书
        when(parserEngine.parseSearchList(
                ArgumentMatchers.eq("<html>闪电漫画的内容</html>"),
                ArgumentMatchers.any())) // 👈 这里去掉了第三个参数
                .thenReturn(List.of(
                        createMockComic("闪电海贼王"),
                        createMockComic("闪电火影")
                ));

        // 收到乌龟漫画的 HTML，吐出 1 本书
        when(parserEngine.parseSearchList(
                ArgumentMatchers.eq("<html>乌龟漫画的内容</html>"),
                ArgumentMatchers.any())) // 👈 这里去掉了第三个参数
                .thenReturn(List.of(
                        createMockComic("乌龟死神")
                ));

        // ================= 4. 鸣枪开跑！=================

        log.info("🔫 测试开始：向 3 个书源发起并发请求...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 执行核心搜索逻辑！
        List<Map<String, String>> finalResult = comicService.search("测试关键字");

        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();
        log.info("🏁 测试结束：总耗时 {} 毫秒，共抓取到 {} 条结果", totalTimeMillis, finalResult.size());

        // ================= 5. 无情断言验证 =================

        // 验证点 1：时间压缩机制生效。总耗时应该接近最慢的乌龟漫画（2000ms），绝对不能是 500+2000=2500ms 以上！
        // 留 200ms 的线程调度误差空间
        assertTrue(totalTimeMillis >= 2000 && totalTimeMillis < 2300,
                "并发失败！耗时不符合预期，总耗时：" + totalTimeMillis);

        // 验证点 2：容错机制生效。虽然“崩溃漫画”抛了异常，但最终结果没有崩，且成功拿到了另外两家的 3 本书。
        assertEquals(3, finalResult.size(), "容错失败！应该正好拿到 3 本漫画");

        // 验证点 3：检查 Service 有没有乖乖给漫画打上狗牌（sourceId）
        long fastCount = finalResult.stream().filter(map -> "1".equals(map.get("sourceId"))).count();
        long slowCount = finalResult.stream().filter(map -> "2".equals(map.get("sourceId"))).count();

        assertEquals(2, fastCount, "闪电漫画应该有 2 条结果");
        assertEquals(1, slowCount, "乌龟漫画应该有 1 条结果");
    }

    // --- 辅助造数据方法 ---
    private ComicSource createMockSource(Integer id, String name, String url) {
        ComicSource source = new ComicSource();
        source.setId(id);
        source.setSourceName(name);
        source.setSourceUrl(url);
        source.setEnable(1);
        ComicSource.RuleSearch rule = new ComicSource.RuleSearch();
        rule.setSearchUrl("/search?q={{key}}");
        source.setRuleSearch(rule);
        return source;
    }

    private Map<String, String> createMockComic(String name) {
        Map<String, String> map = new HashMap<>();
        map.put("name", name);
        return map;
    }
}

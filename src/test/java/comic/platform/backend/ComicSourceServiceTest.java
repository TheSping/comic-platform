package comic.platform.backend;
import comic.platform.backend.module.comic.ComicSource;
import comic.platform.backend.module.comic.ComicSourceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
        import org.springframework.boot.test.context.SpringBootTest;


import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // 🌟 核心：强制指定测试执行顺序
public class ComicSourceServiceTest {


    // 🌟 新增：用来在批量测试的四个步骤之间，传递自动生成的 ID 列表
    private static List<Integer> batchSourceIds = new ArrayList<>();

    @Resource
    private ComicSourceService comicSourceService;

    // 用一个静态变量来在多个测试步骤之间传递新创建的书源 ID
    private static Integer testSourceId;
    private static int initialActiveCount; // 记录最初的启用书源数量（目前应该是 1，包子漫画）

    @Test
    @Order(1)
    @DisplayName("1. 测试初始状态与缓存加载")
    void testGetAllActiveSourcesFromCache() {
        log.info("--- 开始测试：获取活跃书源 ---");
        List<ComicSource> activeSources = comicSourceService.getAllActiveSourcesFromCache();

        assertNotNull(activeSources, "活跃书源列表不应为null");
        initialActiveCount = activeSources.size();
        assertTrue(initialActiveCount >= 1, "初始状态下至少应该有一个启用的书源（包子漫画）");

        log.info("当前启用的书源数量: {}", initialActiveCount);
    }

    @Test
    @Order(2)
    @DisplayName("2. 测试添加书源 (同步刷新缓存)")
    void testAddSource() {
        log.info("--- 开始测试：添加新书源 ---");
        ComicSource newSource = new ComicSource();
        newSource.setSourceName("测试专用漫画源");
        newSource.setSourceUrl("https://www.test-comic.com");
        newSource.setEnable(1); // 默认开启，测试是否能立刻进缓存
        ComicSource.RuleSearch ruleSearch = new ComicSource.RuleSearch();
        ruleSearch.setSearchUrl("/search?q={{key}}");
        ruleSearch.setList("div.comics-card");
        ruleSearch.setName("h3.title > a@text");
        ruleSearch.setAuthor("small.tags@text");
        ruleSearch.setCover("amp-img.cover-img@src");
        ruleSearch.setDetailUrl("h3.title > a@href");

        newSource.setRuleSearch(ruleSearch);

        boolean isAdded = comicSourceService.addSource(newSource);
        assertTrue(isAdded, "添加书源应该返回 true");
        assertNotNull(newSource.getId(), "MyBatis-Plus 插入后应该回填 ID");

        testSourceId = newSource.getId(); // 保存 ID 供后续测试使用

        // 验证缓存是否同步
        List<ComicSource> activeSources = comicSourceService.getAllActiveSourcesFromCache();
        assertEquals(initialActiveCount + 1, activeSources.size(), "添加一个启用的书源后，缓存数量应该 +1");
    }

    @Test
    @Order(3)
    @DisplayName("3. 测试禁用书源 (从缓存中剔除)")
    void testDisableSource() {
        log.info("--- 开始测试：禁用书源 ---");
        boolean isDisabled = comicSourceService.disableSource(testSourceId);
        assertTrue(isDisabled, "禁用书源应该返回 true");

        // 验证缓存是否同步
        List<ComicSource> activeSources = comicSourceService.getAllActiveSourcesFromCache();
        assertEquals(initialActiveCount, activeSources.size(), "禁用该书源后，缓存数量应该恢复到初始值");

        // 确保新加的源确实不在缓存里了
        boolean existsInCache = activeSources.stream().anyMatch(s -> s.getId().equals(testSourceId));
        assertFalse(existsInCache, "被禁用的书源不应该存在于缓存中");
    }

    @Test
    @Order(4)
    @DisplayName("4. 测试启用书源 (重新加入缓存)")
    void testEnableSource() {
        log.info("--- 开始测试：重新启用书源 ---");
        boolean isEnabled = comicSourceService.enableSource(testSourceId);
        assertTrue(isEnabled, "启用书源应该返回 true");

        // 验证缓存是否同步
        List<ComicSource> activeSources = comicSourceService.getAllActiveSourcesFromCache();
        assertEquals(initialActiveCount + 1, activeSources.size(), "重新启用后，缓存数量应该再次 +1");
    }

    @Test
    @Order(5)
    @DisplayName("5. 测试修改书源 (缓存内容更新)")
    void testUpdateSource() {
        log.info("--- 开始测试：更新书源信息 ---");
        // 查出现在的书源（从数据库查，模拟前端传来的实体）
        ComicSource sourceToUpdate = comicSourceService.getById(testSourceId);
        sourceToUpdate.setSourceName("测试专用漫画源-修改版");

        boolean isUpdated = comicSourceService.updateSource(sourceToUpdate);
        assertTrue(isUpdated, "更新书源应该返回 true");

        // 验证缓存中的数据是否也是最新的
        List<ComicSource> activeSources = comicSourceService.getAllActiveSourcesFromCache();
        ComicSource cachedSource = activeSources.stream()
                .filter(s -> s.getId().equals(testSourceId))
                .findFirst()
                .orElse(null);

        assertNotNull(cachedSource, "缓存中应该能找到该书源");
        assertEquals("测试专用漫画源-修改版", cachedSource.getSourceName(), "缓存中的书源名称应该已经被同步修改");
    }

    @Test
    @Order(6)
    @DisplayName("6. 测试手动刷新缓存")
    void testRefreshCache() {
        log.info("--- 开始测试：手动刷新缓存 ---");
        // 随便改一下数据库里的状态，但不调用 updateSource（模拟直接改库的极端情况）
        ComicSource source = new ComicSource();
        source.setId(testSourceId);
        source.setEnable(0);
        comicSourceService.updateById(source); // 绕过我们封装的带缓存的方法，直接用 mybatis-plus 的底层方法

        // 此时缓存还没刷，应该还是启用的
        assertTrue(comicSourceService.getAllActiveSourcesFromCache().stream().anyMatch(s -> s.getId().equals(testSourceId)));

        // 手动刷新！
        comicSourceService.refreshCache();

        // 刷新后，应该消失了
        assertFalse(comicSourceService.getAllActiveSourcesFromCache().stream().anyMatch(s -> s.getId().equals(testSourceId)), "手动刷新后，底层被禁用的书源应该从缓存消失");
    }

    @Test
    @Order(7)
    @DisplayName("7. 测试删除书源 (清理战场)")
    void testDeleteSource() {
        log.info("--- 开始测试：删除书源 ---");
        boolean isDeleted = comicSourceService.deleteSource(testSourceId);
        assertTrue(isDeleted, "删除书源应该返回 true");

        // 验证数据库真的没了
        assertNull(comicSourceService.getById(testSourceId), "数据库中该书源应该已被删除");

        // 验证缓存真的清了
        List<ComicSource> activeSources = comicSourceService.getAllActiveSourcesFromCache();
        assertEquals(initialActiveCount, activeSources.size(), "删除后，缓存数量应该彻底恢复到初始值");
    }

    // ================= 批量操作终极测试 =================

    @Test
    @Order(8)
    @DisplayName("8. 测试批量导入书源 (同步刷新缓存)")
    void testAddSourcesBatch() {
        log.info("--- 开始测试：批量导入书源 ---");
        int beforeCount = comicSourceService.getAllActiveSourcesFromCache().size();

        // 🌟 改进 1：加入时间戳后缀，防止多次跑测试时发生 URL 唯一索引冲突
        String timeSuffix = String.valueOf(System.currentTimeMillis());
        List<ComicSource> newSources = new ArrayList<>();
        List<String> testUrls = new ArrayList<>(); // 记录下我们要去查的特征 URL

        for (int i = 1; i <= 3; i++) {
            ComicSource source = new ComicSource();
            source.setSourceName("批量测试书源-" + i + "-" + timeSuffix);
            String url = "https://batch-test-" + i + "-" + timeSuffix + ".com";
            source.setSourceUrl(url);
            source.setEnable(1);

            // 为了防止 ParserEngine 解析空规则报错，随便塞个简单的规则对象进去
            ComicSource.RuleSearch rule = new ComicSource.RuleSearch();
            rule.setSearchUrl("/search?q={{key}}");
            source.setRuleSearch(rule);

            newSources.add(source);
            testUrls.add(url);
        }

        // 执行批量插入！
        boolean isAdded = comicSourceService.addSourcesBatch(newSources);
        assertTrue(isAdded, "批量导入应该返回 true");

        // 🌟 改进 2：不要信任 saveBatch 会回填 ID，我们自己去数据库查回来！
        List<ComicSource> savedSources = comicSourceService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ComicSource>()
                        .in(ComicSource::getSourceUrl, testUrls)
        );

        batchSourceIds.clear(); // 清空旧数据
        for (ComicSource s : savedSources) {
            batchSourceIds.add(s.getId()); // 100% 能拿到真实的自增 ID
        }
        log.info("成功从数据库反查出批量 ID 列表: {}", batchSourceIds);

        // 验证缓存同步
        int afterCount = comicSourceService.getAllActiveSourcesFromCache().size();
        assertEquals(beforeCount + 3, afterCount, "批量导入 3 个启用书源后，缓存数量应该 +3");
    }

    @Test
    @Order(9)
    @DisplayName("9. 测试批量禁用书源 (从缓存中批量剔除)")
    void testDisableSourcesBatch() {
        log.info("--- 开始测试：批量禁用书源 ---");
        int beforeCount = comicSourceService.getAllActiveSourcesFromCache().size();

        // 执行批量禁用！
        boolean isDisabled = comicSourceService.disableSourcesBatch(batchSourceIds);
        assertTrue(isDisabled, "批量禁用应该返回 true");

        // 验证缓存同步
        int afterCount = comicSourceService.getAllActiveSourcesFromCache().size();
        assertEquals(beforeCount - 3, afterCount, "批量禁用后，缓存数量应该瞬间 -3");
    }

    @Test
    @Order(10)
    @DisplayName("10. 测试批量启用书源 (重新批量加入缓存)")
    void testEnableSourcesBatch() {
        log.info("--- 开始测试：批量启用书源 ---");
        int beforeCount = comicSourceService.getAllActiveSourcesFromCache().size();

        // 执行批量启用！
        boolean isEnabled = comicSourceService.enableSourcesBatch(batchSourceIds);
        assertTrue(isEnabled, "批量启用应该返回 true");

        // 验证缓存同步
        int afterCount = comicSourceService.getAllActiveSourcesFromCache().size();
        assertEquals(beforeCount + 3, afterCount, "批量启用后，缓存数量应该恢复 +3");
    }

    @Test
    @Order(11)
    @DisplayName("11. 测试批量删除书源 (清理批量战场)")
    void testDeleteSourcesBatch() {
        log.info("--- 开始测试：批量删除书源 ---");
        int beforeCount = comicSourceService.getAllActiveSourcesFromCache().size();

        // 执行批量彻底删除！
        boolean isDeleted = comicSourceService.deleteSourcesBatch(batchSourceIds);
        assertTrue(isDeleted, "批量删除应该返回 true");

        // 验证数据库真的没了 (任意抽查一个)
        assertNull(comicSourceService.getById(batchSourceIds.get(0)), "数据库中该书源应该已被批量删除");

        // 验证缓存同步
        int afterCount = comicSourceService.getAllActiveSourcesFromCache().size();
        assertEquals(beforeCount - 3, afterCount, "批量删除后，这 3 个书源必须从内存中彻底消失");
    }



}

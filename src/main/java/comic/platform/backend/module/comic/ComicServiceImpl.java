package comic.platform.backend.module.comic;

import comic.platform.backend.core.exception.ComicException;
import comic.platform.backend.module.comic.dto.SearchMorePage;
import comic.platform.backend.module.comic.dto.SearchResult;
import comic.platform.backend.module.comic.dto.TocResult;
import comic.platform.backend.module.comic.parser.ParserEngine;
import comic.platform.backend.service.NetworkService;
import comic.platform.backend.util.UrlUtils;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
public class ComicServiceImpl implements ComicService {

    @Resource
    private ComicSourceService comicSourceService;
    @Resource
    private ComicSourceMapper comicSourceMapper;
    @Resource
    private NetworkService networkService;
    @Resource
    private ParserEngine parserEngine;
    @Resource(name = "crawlerExecutor")
    private ExecutorService crawlerExecutor;
    @Resource
    @Lazy
    private ComicServiceImpl self;


    @Override
    @SneakyThrows
    public List<SearchResult> search(@RequestParam("keyword") String keyword) {

        List<ComicSource> activeSources = comicSourceService.getAllActiveSourcesFromCache();
        if (activeSources == null || activeSources.isEmpty()) {
            throw new ComicException(400, "当前系统没有可用的漫画源，请先在后台配置");
        }
        log.info("开始并发搜索关键词: {}，并发线程数: {}", keyword, activeSources.size());

        // 为所有启用的书源分发线程，并发处理。注意：此处改用 self.searchSingleSource 触发缓存机制
        List<CompletableFuture<SearchResult>> futures = activeSources
                .stream()
                .map(source -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return self.searchSingleSource(source, keyword);
                    } catch (Exception e) {
                        log.error("书源 [{}] 搜索异常: {}", source.getSourceName(), e.getMessage());
                        return null;
                    }
                }, crawlerExecutor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join) // 这一步隐式地包含了等待机制，并安全拿出每个线程的 List
                .filter(java.util.Objects::nonNull) // 剔除掉所有失败返回 null 的书源
                .toList();
    }

    /**
     * 单个书源的搜索逻辑（被提取出来用于精细化进行 Redis 缓存）
     * Key 生成策略：使用 Spring 内置的 DigestUtils 将关键词转为 MD5 散列值
     */
    @Cacheable(value = "search", key = "'search:' + @cacheKeyUtil.md5(#keyword) + ':' + #source.id", sync = true)
    public SearchResult searchSingleSource(ComicSource source, String keyword) {
        log.info("Redis 缓存未命中，开始从源站抓取搜索结果。书源: {}, 关键词: {}", source.getSourceName(), keyword);
        String searchPath = UrlUtils.renderUrl(source.getRuleSearch().getSearchUrl(), keyword);
        String targetUrl = UrlUtils.resolveUrl(source.getSourceUrl(), searchPath);

        String html = networkService.getHtml(targetUrl);
        if (html == null || html.isEmpty()) return null;

        SearchResult sourceResult = parserEngine.parseSearchList(html, source.getRuleSearch(), source.getSourceUrl());

        if (sourceResult != null) {
            sourceResult.setSourceId(source.getId());
            sourceResult.setSourceName(source.getSourceName());
            return sourceResult;
        }
        return null;
    }

    /**
     * 并发批量加载下一页
     */
    @Override
    @SneakyThrows
    public List<SearchResult> searchNextPageBatch(List<SearchMorePage> requests) {
        if (requests == null || requests.isEmpty()) return Collections.emptyList();
        log.info("开始并发翻页，共收到 {} 个书源的翻页请求", requests.size());

        List<CompletableFuture<SearchResult>> futures = requests
                .stream()
                .map(req -> CompletableFuture.supplyAsync(() -> {
                    try {
                        ComicSource source = comicSourceService.getActiveSourceByIdFromCache(req.getSourceId());
                        if (source == null) return null;

                        // 使用自我注入的 self 代理对象去调用单体翻页方法，触发 Redis 缓存
                        return self.searchSingleNextPage(req, source);

                    } catch (Exception e) {
                        log.error("书源 ID [{}] 翻页异常: {}", req.getSourceId(), e.getMessage());
                        return null;

                    }
                }, crawlerExecutor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * 单个书源的翻页逻辑（被提取出来用于精细化 Redis 缓存）
     * Key 结构: search_page:{url_hash}:{sourceId}
     */
    @Cacheable(value = "search_page", key = "'search_page:' + @cacheKeyUtil.md5(#req.nextPageUrl) + ':' + #req.sourceId", sync = true)
    public SearchResult searchSingleNextPage(SearchMorePage req, ComicSource source) {
        log.info("Redis 缓存未命中，开始从源站抓取下一页。书源: {}, URL: {}", source.getSourceName(), req.getNextPageUrl());

        String targetUrl = UrlUtils.resolveUrl(source.getSourceUrl(), req.getNextPageUrl());
        String html = networkService.getHtml(targetUrl);
        if (html == null || html.isEmpty()) return null;

        SearchResult result = parserEngine.parseSearchList(html, source.getRuleSearch(), source.getSourceUrl());

        if (result != null) {
            result.setSourceId(source.getId());
            result.setSourceName(source.getSourceName());
        }
        return result;
    }

    /**
     * 目录页解析
     * Key 结构: toc:{detailUrl_hash}:{sourceId}
     */
    @Override
    @SneakyThrows
    @Cacheable(value = "toc", key = "'toc:' + @cacheKeyUtil.md5(#detailUrl) + ':' + #sourceId", sync = true)
    public TocResult getToc(@RequestParam("url") String detailUrl, Integer sourceId) {
        log.info("Redis 缓存未命中，开始抓取目录。书源ID: {}, URL: {}", sourceId, detailUrl);
        ComicSource source = comicSourceService.getActiveSourceByIdFromCache(sourceId);
        if (source == null) {
            throw new ComicException(404, "书源不存在或已被禁用");
        }
        String targetUrl = UrlUtils.resolveUrl(source.getSourceUrl(), detailUrl);
        String html = networkService.getHtml(targetUrl);
        if (html == null || html.isEmpty()) {
            throw new ComicException(500, "目标漫画网站无响应或被防爬虫拦截！");
        }
        // 拿到包含当页章节和下一页的复合对象
        TocResult result = parserEngine.parseTocList(html, source.getRuleToc());
        if (result == null || result.getChapters() == null || result.getChapters().isEmpty()) {
            throw new ComicException(404, "未找到目录");
        }
        // 如果正则没抓到下一页，把它设为 null
        if (result.getNextPageUrl() == null || result.getNextPageUrl().isEmpty()) {
            result.setNextPageUrl(null);
        }

        for (Map<String, String> item : result.getChapters()) {
            item.put("sourceId", String.valueOf(source.getId()));
            item.put("sourceName", source.getSourceName());
        }

        return result;
    }

    /**
     * 图片页解析
     * Key 结构: content:{chapterUrl_hash}:{sourceId}
     */
    @Override
    @SneakyThrows
    @Cacheable(value = "content", key = "'content:' + @cacheKeyUtil.md5(#chapterUrl) + ':' + #sourceId", sync = true)
    public List<String> getContent(@RequestParam("url") String chapterUrl, Integer sourceId) {
        log.info("Redis 缓存未命中，开始抓取图片内容。书源ID: {}, URL: {}", sourceId, chapterUrl);
        ComicSource source = comicSourceService.getActiveSourceByIdFromCache(sourceId);
        if (source == null) {
            throw new ComicException(404, "书源不存在或已被禁用");
        }
        String targetUrl = UrlUtils.resolveUrl(source.getSourceUrl(), chapterUrl);
        String html = networkService.getHtml(targetUrl);
        if (html == null || html.isEmpty()) {
            throw new ComicException(500, "目标漫画网站无响应或被防爬虫拦截！");
        }
        List<String> result = parserEngine.parseContent(html, source.getRuleContent());
        if (result == null || result.isEmpty()) {
            throw new ComicException(404, "未找到内容");
        }
        return result;
    }
}

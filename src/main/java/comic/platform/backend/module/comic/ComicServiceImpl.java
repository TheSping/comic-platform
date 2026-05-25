package comic.platform.backend.module.comic;

import comic.platform.backend.core.exception.ComicException;
import comic.platform.backend.module.comic.parser.ParserEngine;
import comic.platform.backend.service.NetworkService;
import comic.platform.backend.util.UrlUtils;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
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

    /**
     * 搜索页解析
     */
    @Override
    @SneakyThrows
    public List<Map<String, String>> search(@RequestParam("keyword") String keyword) {

        List<ComicSource> activeSources = comicSourceService.getAllActiveSourcesFromCache();
        if (activeSources == null || activeSources.isEmpty()) {
            throw new ComicException(400, "当前系统没有可用的漫画源，请先在后台配置");
        }
        log.info("开始并发搜索关键词: {}，并发线程数: {}", keyword, activeSources.size());

        //为所有启用的书源分发线程，并发处理
        List<CompletableFuture<List<Map<String, String>>>> futures = activeSources.stream()
                .map(source -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String searchPath = UrlUtils.renderUrl(source.getRuleSearch().getSearchUrl(), keyword);
                        String targetUrl = UrlUtils.resolveUrl(source.getSourceUrl(), searchPath);

                        String html = networkService.getHtml(targetUrl);
                        if (html == null || html.isEmpty()) return Collections.<Map<String, String>>emptyList();

                        List<Map<String, String>> sourceResult = parserEngine.parseSearchList(html, source.getRuleSearch());

                        if (sourceResult != null) {
                            for (Map<String, String> item : sourceResult) {
                                item.put("sourceId", String.valueOf(source.getId()));
                                item.put("sourceName", source.getSourceName());
                            }
                            return sourceResult;
                        }
                        return Collections.<Map<String, String>>emptyList();

                    } catch (Exception e) {
                        log.error("书源 [{}] 搜索异常: {}", source.getSourceName(), e.getMessage());
                        return Collections.<Map<String, String>>emptyList();
                    }
                }, crawlerExecutor))
                .toList();

        // 等待所有子线程
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 遍历汇总
        List<Map<String, String>> Result = new ArrayList<>();
        for (CompletableFuture<List<Map<String, String>>> future : futures) {
            try {
                Result.addAll(future.get());
            } catch (Exception e) {
                log.error("汇总子线程结果出错", e);
            }
        }

        return Result;
    }

    /**
     * 目录页解析
     */
    @Override
    @SneakyThrows
    public List<Map<String, String>> getToc(@RequestParam("url") String detailUrl, Integer sourceId) {
        ComicSource source = comicSourceService.getActiveSourceByIdFromCache(sourceId);
        if (source == null) {
            throw new ComicException(404, "书源不存在或已被禁用");
        }
        String targetUrl = UrlUtils.resolveUrl(source.getSourceUrl(), detailUrl);
        String html = networkService.getHtml(targetUrl);
        if (html == null || html.isEmpty()) {
            throw new ComicException(500, "目标漫画网站无响应或被防爬虫拦截！");
        }
        List<Map<String, String>> result = parserEngine.parseTocList(html, source.getRuleToc());
        if (result == null || result.isEmpty()) {
            throw new ComicException(404, "未找到目录");
        }

        for (Map<String, String> item : result) {
            item.put("sourceId", String.valueOf(source.getId()));
            item.put("sourceName", source.getSourceName());
        }

        return result;
    }

    /**
     * 图片页解析
     */
    @Override
    @SneakyThrows
    public List<String> getContent(@RequestParam("url") String chapterUrl, Integer sourceId) {
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

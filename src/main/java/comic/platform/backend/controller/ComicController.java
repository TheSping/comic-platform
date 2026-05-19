package comic.platform.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import comic.platform.backend.entity.ComicSource;
import comic.platform.backend.mapper.ComicSourceMapper;
import comic.platform.backend.parser.ParserEngine;
import comic.platform.backend.service.NetworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/comic")
@RequiredArgsConstructor
// 解决本地 Vue 调试时的跨域问题（如果你的 Vue 用了 Vite 代理，这行可以不加）
@CrossOrigin(origins = "*")
public class ComicController {

    private final ComicSourceMapper comicSourceMapper;
    private final NetworkService networkService;
    private final ParserEngine parserEngine;

    // 工具方法：获取当前测试的默认书源（包子漫画）
    // 等以后做聚合搜索时，只需要在参数里加一个 sourceId 传过来即可
    private ComicSource getDefaultSource() {
        QueryWrapper<ComicSource> wrapper = new QueryWrapper<>();
        wrapper.eq("source_name", "包子漫画");
        ComicSource source = comicSourceMapper.selectOne(wrapper);
        if (source == null) {
            throw new RuntimeException("数据库中未找到包子漫画书源！");
        }
        return source;
    }

    // 工具方法：处理相对路径，拼成绝对路径
    private String getAbsoluteUrl(String baseUrl, String path) {
        if (path == null || path.isEmpty()) return "";
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path; // 已经是绝对路径
        }
        // 如果 path 不是以 / 开头，且 baseUrl 不以 / 结尾，补充 /
        if (!path.startsWith("/") && !baseUrl.endsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }


    /**
     * 路由一：搜索接口
     * GET /api/comic/search?keyword=海贼王
     */
    @GetMapping("/search")
    public List<Map<String, String>> search(@RequestParam("keyword") String keyword) {
        ComicSource source = getDefaultSource();

        try {
            // 1. 拼装搜索 URL 并编码关键词
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.name());
            String searchPath = source.getRuleSearch().getSearchUrl().replace("{{key}}", encodedKeyword);
            String targetUrl = getAbsoluteUrl(source.getSourceUrl(), searchPath);

            // 2. 网络抓取
            String html = networkService.getHtml(targetUrl);

            // 3. 引擎解析
            return parserEngine.parseSearchList(html, source.getRuleSearch());

        } catch (Exception e) {
            log.error("搜索异常，关键词：{}", keyword, e);
            return new ArrayList<>(); // 真实项目里这里建议返回你封装的 RestBean 错误码
        }
    }


    /**
     * 路由二：获取目录接口
     * GET /api/comic/toc?url=/comic/haizeiwang
     */
    @GetMapping("/toc")
    public List<Map<String, String>> getToc(@RequestParam("url") String detailUrl) {
        ComicSource source = getDefaultSource();

        try {
            // 1. 拼装绝对路径
            String targetUrl = getAbsoluteUrl(source.getSourceUrl(), detailUrl);

            // 2. 网络抓取
            String html = networkService.getHtml(targetUrl);

            // 3. 引擎解析
            return parserEngine.parseTocList(html, source.getRuleToc());

        } catch (Exception e) {
            log.error("获取目录异常，URL：{}", detailUrl, e);
            return new ArrayList<>();
        }
    }


    /**
     * 路由三：获取正文图片接口
     * GET /api/comic/content?url=/comic/haizeiwang/ch1
     */
    @GetMapping("/content")
    public List<String> getContent(@RequestParam("url") String chapterUrl) {
        ComicSource source = getDefaultSource();

        try {
            // 1. 拼装绝对路径
            String targetUrl = getAbsoluteUrl(source.getSourceUrl(), chapterUrl);

            // 2. 网络抓取
            String html = networkService.getHtml(targetUrl);

            // 3. 引擎解析，拿到原图 URL 列表
            return parserEngine.parseContent(html, source.getRuleContent());

        } catch (Exception e) {
            log.error("获取正文异常，URL：{}", chapterUrl, e);
            return new ArrayList<>();
        }
    }
}

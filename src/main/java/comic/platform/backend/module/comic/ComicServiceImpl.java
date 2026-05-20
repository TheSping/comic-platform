package comic.platform.backend.module.comic;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import comic.platform.backend.entity.ComicSource;
import comic.platform.backend.core.exception.ComicException;
import comic.platform.backend.module.comic.parser.ParserEngine;
import comic.platform.backend.service.NetworkService;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ComicServiceImpl implements ComicService {

    @Resource
    private ComicSourceMapper comicSourceMapper;
    @Resource
    private NetworkService networkService;
    @Resource
    private ParserEngine parserEngine;

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

    @Override
    @SneakyThrows
    public List<Map<String, String>> search(@RequestParam("keyword") String keyword) {
        ComicSource source = getDefaultSource();
        // 拼装
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String searchPath = source.getRuleSearch().getSearchUrl().replace("{{key}}", encodedKeyword);
        String targetUrl = getAbsoluteUrl(source.getSourceUrl(), searchPath);

        // 抓取
        String html = networkService.getHtml(targetUrl);
        if (html == null || html.isEmpty()) {
            throw new ComicException(5001, "目标漫画网站无响应或被防爬虫拦截！");
        }

        // 解析
        List<Map<String, String>> result = parserEngine.parseSearchList(html, source.getRuleSearch());
        if (result == null || result.isEmpty()) {
            throw new ComicException(4004, "未找到漫画《" + keyword + "》，或该站规则已失效需更新！");
        }

        return result;

    }

    @Override
    @SneakyThrows
    public List<Map<String, String>> getToc(@RequestParam("url") String detailUrl) {
        ComicSource source = getDefaultSource();
        String targetUrl = getAbsoluteUrl(source.getSourceUrl(), detailUrl);
        String html = networkService.getHtml(targetUrl);
        if (html == null || html.isEmpty()) {
            throw new ComicException(5001, "目标漫画网站无响应或被防爬虫拦截！");
        }
        List<Map<String, String>> result = parserEngine.parseTocList(html, source.getRuleToc());
        if (result == null || result.isEmpty()) {
            throw new ComicException(4004, "未找到目录，或该站规则已失效需更新！");
        }
        return result;
    }

    @Override
    @SneakyThrows
    public List<String> getContent(@RequestParam("url") String chapterUrl) {
        ComicSource source = getDefaultSource();
        String targetUrl = getAbsoluteUrl(source.getSourceUrl(), chapterUrl);
        String html = networkService.getHtml(targetUrl);
        if (html == null || html.isEmpty()) {
            throw new ComicException(5001, "目标漫画网站无响应或被防爬虫拦截！");
        }
        List<String> result = parserEngine.parseContent(html, source.getRuleContent());
        if (result == null || result.isEmpty()) {
            throw new ComicException(4004, "未找到内容，或该站规则已失效需更新！");
        }
        return result;
    }
}

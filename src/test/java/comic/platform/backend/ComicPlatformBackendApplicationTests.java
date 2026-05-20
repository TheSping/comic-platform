package comic.platform.backend;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import comic.platform.backend.entity.ComicSource;
import comic.platform.backend.module.comic.ComicSourceMapper;
import comic.platform.backend.module.comic.parser.ParserEngine;
import comic.platform.backend.service.NetworkService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@SpringBootTest
class ComicPlatformBackendApplicationTests {

    @Resource
    private ComicSourceMapper comicSourceMapper;
    @Resource
    private NetworkService networkService;
    @Resource
    private ParserEngine parserEngine;

    @Test
    public void testBaoziMangaFlow() throws Exception {
        System.out.println("========== 🌍 真实世界抓取引擎启动 🌍 ==========");

        // 1. 从数据库读取包子漫画的规则
        QueryWrapper<ComicSource> wrapper = new QueryWrapper<>();
        wrapper.eq("source_name", "包子漫画");
        ComicSource source = comicSourceMapper.selectOne(wrapper);

        if (source == null) {
            System.out.println("❌ 没找到书源，请确认 SQL 是否执行成功！");
            return;
        }

        // ==========================================
        // ⚔️ 第一战：搜索《海贼王》
        // ==========================================
        String keyword = "海贼王";
        //{"list": "div.comics-card", "name": "a.comics-card__poster@title", "cover": "amp-img@src", "detailUrl": "a.comics-card__poster@href", "searchUrl": "/search?q={{key}}"}
        // 把规则里的 {{key}} 替换成真正要搜的词（记得转码，否则乱码搜不出）
        String searchPath = source.getRuleSearch().getSearchUrl()
                .replace("{{key}}", URLEncoder.encode(keyword, StandardCharsets.UTF_8.name()));
        String searchUrl = source.getSourceUrl() + searchPath;

        System.out.println("\n[1/3] 正在向目标发起搜索请求：" + searchUrl);
        String searchHtml = networkService.getHtml(searchUrl);

        if (searchHtml.isEmpty()) {
            System.out.println("❌ 搜索页抓取失败！可能遇到了防爬拦截（如 Cloudflare 5秒盾）。");
            return;
        }

        List<Map<String, String>> searchResult = parserEngine.parseSearchList(searchHtml, source.getRuleSearch());

        if (searchResult.isEmpty()) {
            System.out.println("❌ 搜索页解析出错了！可能是包子漫画今天改了前端代码。");
            return;
        }

        Map<String, String> firstBook = searchResult.get(0);
        System.out.println("✅ 斩获目标！书名: " + firstBook.get("name"));
        System.out.println("   封面链接: " + firstBook.get("cover"));


        // ==========================================
        // ⚔️ 第二战：杀入目录页
        // ==========================================
        // 注意：Legado 抓出来的 detailUrl 可能是绝对路径也可能是相对路径
        String detailPath = firstBook.get("detailUrl");
        String tocUrl = detailPath.startsWith("http") ? detailPath : source.getSourceUrl() + detailPath;

        System.out.println("\n[2/3] 正在潜入目录页：" + tocUrl);
        String tocHtml = networkService.getHtml(tocUrl);
        List<Map<String, String>> tocResult = parserEngine.parseTocList(tocHtml, source.getRuleToc());

        System.out.println("✅ 成功夺取目录！共发现 " + tocResult.size() + " 话。");
        if (tocResult.isEmpty()) return;

        Map<String, String> firstChapter = tocResult.get(0);
        System.out.println("   准备阅读: " + firstChapter.get("name"));


        // ==========================================
        // ⚔️ 第三战：强夺正文图片
        // ==========================================
        String chapterPath = firstChapter.get("url");
        String contentUrl = chapterPath.startsWith("http") ? chapterPath : source.getSourceUrl() + chapterPath;

        System.out.println("\n[3/3] 正在解析漫画图片：" + contentUrl);
        String contentHtml = networkService.getHtml(contentUrl);
        List<String> images = parserEngine.parseContent(contentHtml, source.getRuleContent());

        System.out.println("✅ 大获全胜！共提取到 " + images.size() + " 张漫画原图链接：");

        // 只打印前 3 张意思一下
        for (int i = 0; i < Math.min(3, images.size()); i++) {
            System.out.println("   原图 " + (i+1) + ": " + images.get(i));
            System.out.println("   http://localhost:8081/api/proxy/image?url=" + images.get(i));
        }
    }

}

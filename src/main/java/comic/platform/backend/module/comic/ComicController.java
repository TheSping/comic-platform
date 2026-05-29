package comic.platform.backend.module.comic;

import comic.platform.backend.core.RestBean;
import comic.platform.backend.module.bookshelf.BookshelfService;
import comic.platform.backend.module.comic.dto.SearchMorePage;
import comic.platform.backend.module.comic.dto.SearchResult;
import comic.platform.backend.module.comic.dto.TocResult;
import comic.platform.backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Slf4j
@RestController
@RequestMapping("/api/comic")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Validated
@Tag(name = "漫画核心业务", description = "漫画聚合搜索、目录解析、内容阅读及自动进度控制")
public class ComicController {

    @Resource
    ComicService comicService;

    @Resource
    BookshelfService bookshelfService;

    @Operation(summary = "1. 聚合搜索 (首页)", description = "输入关键字，多线程并发搜索所有启用的漫画源，返回带源分组结构的第一页结果。")
    @Parameter(name = "keyword", description = "搜索关键字", required = true, example = "海贼王")
    @GetMapping("/search")
    public RestBean<List<SearchResult>> search(
            @RequestParam("keyword") @NotBlank(message = "搜索关键字不能为空") @Size(max = 100, message = "搜索关键字长度不能超过100") String keyword) {

        List<SearchResult> result = comicService.search(keyword);
        return RestBean.success(result);
    }

    @Operation(summary = "2. 并发批量翻页", description = "前端将第一页搜出的 nextPageUrl 集合传回，后端并发拉取各书源的下一页，实现聚合瀑布流。")
    @PostMapping("/search/page/batch")
    public RestBean<List<SearchResult>> searchNextPageBatch(@RequestBody List<SearchMorePage> requests) {
        List<SearchResult> result = comicService.searchNextPageBatch(requests);
        return RestBean.success(result);
    }

    @Operation(summary = "3. 获取漫画目录", description = "传入漫画详情URL和来源ID，解析出该漫画的完整章节目录列表及潜在的目录下一页。")
    @Parameters({
            @Parameter(name = "detailUrl", description = "漫画详情页URL(相对或绝对路径)", required = true, example = "/comic/haizeiwang"),
            @Parameter(name = "sourceId", description = "漫画书源ID", required = true, example = "1")
    })
    @GetMapping("/toc")
    public RestBean<TocResult> getToc(
            @RequestParam("url") @NotBlank(message = "漫画详情URL不能为空") String detailUrl,
            @RequestParam("sourceId") Integer sourceId) {

        TocResult result = comicService.getToc(detailUrl, sourceId);
        return RestBean.success(result);
    }

    @Operation(summary = "4. 获取章节图片 (并记录进度)", description = "解析具体章节，返回图片URL数组。若传齐参数则会自动异步更新用户书架阅读进度。")
    @Parameters({
            @Parameter(name = "url", description = "具体章节的阅读页URL", required = true, example = "/chapter/haizeiwang/1"),
            @Parameter(name = "sourceId", description = "漫画书源ID", required = true, example = "1"),
            @Parameter(name = "chapterName", description = "章节名称 (选填，用于记录到书架)", example = "第1话"),
            @Parameter(name = "detailUrl", description = "漫画详情URL (选填，用于匹配书架中的漫画)", example = "/comic/haizeiwang")
    })
    @GetMapping("/content")
    public RestBean<List<String>> getContent(
            @RequestParam("url") @NotBlank(message = "章节URL不能为空") String chapterUrl,
            @RequestParam("sourceId") Integer sourceId,
            @RequestParam(value = "chapterName", required = false) String chapterName,
            @RequestParam(value = "detailUrl", required = false) String detailUrl) {

        List<String> result = comicService.getContent(chapterUrl, sourceId);
        Integer userId = SecurityUtils.getUserId();
        if (detailUrl != null && chapterName != null) {
            bookshelfService.autoSaveProgress(userId, sourceId, detailUrl, chapterName, chapterUrl);
        }

        return RestBean.success(result);
    }
}
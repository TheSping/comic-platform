package comic.platform.backend.module.comic;

import comic.platform.backend.core.RestBean;
import comic.platform.backend.module.bookshelf.BookshelfService;
import comic.platform.backend.module.comic.dto.TocResult;
import comic.platform.backend.util.SecurityUtils;
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
public class ComicController {

    @Resource
    ComicService comicService;

    @Resource
    BookshelfService bookshelfService;

    @Resource(name = "asyncTaskExecutor")
    private Executor asyncTaskExecutor;

    @GetMapping("/search")
    public RestBean<List<Map<String, String>>> search(
            @RequestParam("keyword") @NotBlank(message = "搜索关键字不能为空") @Size(max = 100, message = "搜索关键字长度不能超过100") String keyword) {

        List<Map<String, String>> result = comicService.search(keyword);
        return RestBean.success(result);
    }

    @GetMapping("/toc")
    public RestBean<TocResult> getToc(
            @RequestParam("url") @NotBlank(message = "漫画详情URL不能为空") String detailUrl,
            @RequestParam("sourceId") Integer sourceId) {

        TocResult result = comicService.getToc(detailUrl, sourceId);
        return RestBean.success(result);
    }

    @GetMapping("/content")
    public RestBean<List<String>> getContent(
            @RequestParam("url") @NotBlank(message = "章节URL不能为空") String chapterUrl,
            @RequestParam("sourceId") Integer sourceId,
            @RequestParam(value = "chapterName", required = false) String chapterName,
            @RequestParam(value = "detailUrl", required = false) String detailUrl) {

        List<String> result = comicService.getContent(chapterUrl, sourceId);

        //进度保存
        try {
            Integer userId = SecurityUtils.getUserId();
            if (detailUrl != null && chapterName != null) {
                bookshelfService.autoSaveProgress(userId, sourceId, detailUrl, chapterName, chapterUrl);
            }
        } catch (Exception e) {
            // 用户未登录，什么都不做，直接往下走
        }

        return RestBean.success(result);
    }
}
package comic.platform.backend.module.comic;

import comic.platform.backend.entity.RestBean;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated; // 注意这个包
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/comic")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Validated
public class ComicController {

    @Resource
    ComicService comicService;

    @GetMapping("/search")
    public RestBean<List<Map<String, String>>> search(
            @RequestParam("keyword")
            @NotBlank(message = "搜索关键字不能为空")
            @Size(max = 100, message = "搜索关键字长度不能超过100") String keyword) {

        List<Map<String, String>> result = comicService.search(keyword);
        return RestBean.success(result);
    }

    @GetMapping("/toc")
    public RestBean<List<Map<String, String>>> getToc(
            @RequestParam("url")
            @NotBlank(message = "漫画详情URL不能为空") String detailUrl,
            @RequestParam("sourceId") Integer sourceId) {

        List<Map<String, String>> result = comicService.getToc(detailUrl, sourceId);
        return RestBean.success(result);
    }

    @GetMapping("/content")
    public RestBean<List<String>> getContent(
            @RequestParam("url")
            @NotBlank(message = "章节URL不能为空") String chapterUrl,
            @RequestParam("sourceId") Integer sourceId) {

        List<String> result = comicService.getContent(chapterUrl, sourceId);
        return RestBean.success(result);
    }
}
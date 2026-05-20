package comic.platform.backend.module.comic;

import comic.platform.backend.entity.RestBean;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/comic")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ComicController {

    @Resource
    ComicService comicService;


    /**
     * 路由一：搜索接口
     * GET /api/comic/search?keyword=海贼王
     */
    @GetMapping("/search")
    public RestBean<List<Map<String, String>>> search(@RequestParam("keyword") String keyword) {
        List<Map<String, String>> result = comicService.search(keyword);
        return RestBean.success(result);
    }


    /**
     * 路由二：获取目录接口
     * GET /api/comic/toc?url=/comic/haizeiwang
     */
    @GetMapping("/toc")
    public RestBean<List<Map<String, String>>> getToc(@RequestParam("url") String detailUrl) {
        List<Map<String, String>> result = comicService.getToc(detailUrl);
        return RestBean.success(result);
    }


    /**
     * 路由三：获取正文图片接口
     * GET /api/comic/content?url=/comic/haizeiwang/ch1
     */
    @GetMapping("/content")
    public RestBean<List<String>> getContent(@RequestParam("url") String chapterUrl) {
        List<String> result = comicService.getContent(chapterUrl);
        return RestBean.success(result);
    }
}

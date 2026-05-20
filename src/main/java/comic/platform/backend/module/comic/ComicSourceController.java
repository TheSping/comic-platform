package comic.platform.backend.module.comic;

import comic.platform.backend.entity.ComicSource;
import comic.platform.backend.entity.RestBean;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/source")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ComicSourceController {

    private final ComicSourceService comicSourceService;

    /**
     * 查：获取所有书源列表
     * GET /api/source/list
     */
    @GetMapping("/list")
    public RestBean<List<ComicSource>> list() {
        List<ComicSource> sourceList = comicSourceService.list();
        return RestBean.success(sourceList);
    }

    /**
     * 查：根据 ID 获取单个书源详情
     * GET /api/source/1
     */
    @GetMapping("/{id}")
    public RestBean<ComicSource> getById(@PathVariable("id") Integer id) {
        ComicSource source = comicSourceService.getById(id);
        return source != null ? RestBean.success(source) : RestBean.failure(404, "书源不存在");
    }

    /**
     * 增：添加新书源
     * POST /api/source
     * 注意：使用 @RequestBody 接收前端传来的 JSON 数据
     */
    @PostMapping
    public RestBean<String> add(@RequestBody ComicSource source) {
        boolean success = comicSourceService.save(source);
        return success ? RestBean.success("添加成功") : RestBean.failure(500, "添加失败");
    }

    /**
     * 改：修改已有书源
     * PUT /api/source
     */
    @PutMapping
    public RestBean<String> update(@RequestBody ComicSource source) {
        // updateById() 会根据传入对象的 id 字段去更新其他非空字段
        boolean success = comicSourceService.updateById(source);
        return success ? RestBean.success("更新成功") : RestBean.failure(500, "更新失败，请检查ID是否存在");
    }

    /**
     * 删：根据 ID 删除书源
     * DELETE /api/source/1
     */
    @DeleteMapping("/{id}")
    public RestBean<String> delete(@PathVariable("id") Integer id) {
        boolean success = comicSourceService.removeById(id);
        return success ? RestBean.success("删除成功") : RestBean.failure(500, "删除失败");
    }
}

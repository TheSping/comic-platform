package comic.platform.backend.module.comic;

import comic.platform.backend.core.RestBean;
import jakarta.validation.Valid;
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
     * 查：获取所有书源列表（支持按名称模糊搜索）
     * GET /api/source/list
     */
    @GetMapping("/list")
    public RestBean<List<ComicSource>> list(@RequestParam(required = false) String keyword) {
        List<ComicSource> list = comicSourceService.queryAdminSources(keyword);
        return RestBean.success(list);
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
     */
    @PostMapping
    public RestBean<String> add(@Valid @RequestBody ComicSource source) {
        boolean success = comicSourceService.addSource(source);
        return success ? RestBean.success("添加成功") : RestBean.failure(500, "添加失败");
    }

    /**
     * 改：修改已有书源
     * PUT /api/source
     */
    @PutMapping
    public RestBean<String> update(@Valid @RequestBody ComicSource source) {
        boolean success = comicSourceService.updateSource(source);
        return success ? RestBean.success("更新成功") : RestBean.failure(500, "更新失败，请检查ID是否存在");
    }

    /**
     * 删：根据 ID 删除书源
     * DELETE /api/source/1
     */
    @DeleteMapping("/{id}")
    public RestBean<String> delete(@PathVariable("id") Integer id) {
        boolean success = comicSourceService.deleteSource(id);
        return success ? RestBean.success("删除成功") : RestBean.failure(500, "删除失败");
    }


    /**
     * 启用书源
     * PUT /api/source/1/enable
     */
    @PutMapping("/{id}/enable")
    public RestBean<String> enableSource(@PathVariable("id") Integer id) {
        boolean success = comicSourceService.enableSource(id);
        return success ? RestBean.success("书源已启用") : RestBean.failure(500, "启用失败，请检查书源ID");
    }

    /**
     * 禁用书源
     * PUT /api/source/1/disable
     */
    @PutMapping("/{id}/disable")
    public RestBean<String> disableSource(@PathVariable("id") Integer id) {
        boolean success = comicSourceService.disableSource(id);
        return success ? RestBean.success("书源已禁用") : RestBean.failure(500, "禁用失败，请检查书源ID");
    }

    /**
     * 批量导入书源
     * POST /api/source/batch
     */
    @PostMapping("/batch")
    public RestBean<String> addBatch(@RequestBody List<ComicSource> sources) {
        boolean success = comicSourceService.addSourcesBatch(sources);
        return success ? RestBean.success("批量导入成功") : RestBean.failure(500, "批量导入失败");
    }

    /**
     * 批量删除书源
     * POST /api/source/batch/delete
     * 注意：虽然按照 REST 语义该用 DELETE，但某些老旧前端框架对 DELETE 请求带 Body 支持不好，
     * 所以在批量操作时，常用 POST + 动作路径。
     */
    @PostMapping("/batch/delete")
    public RestBean<String> deleteBatch(@RequestBody List<Integer> ids) {
        boolean success = comicSourceService.deleteSourcesBatch(ids);
        return success ? RestBean.success("批量删除成功") : RestBean.failure(500, "批量删除失败");
    }

    /**
     * 批量启用书源
     * PUT /api/source/batch/enable
     */
    @PutMapping("/batch/enable")
    public RestBean<String> enableBatch(@RequestBody List<Integer> ids) {
        boolean success = comicSourceService.enableSourcesBatch(ids);
        return success ? RestBean.success("批量启用成功") : RestBean.failure(500, "批量启用失败");
    }

    /**
     * 批量禁用书源
     * PUT /api/source/batch/disable
     */
    @PutMapping("/batch/disable")
    public RestBean<String> disableBatch(@RequestBody List<Integer> ids) {
        boolean success = comicSourceService.disableSourcesBatch(ids);
        return success ? RestBean.success("批量禁用成功") : RestBean.failure(500, "批量禁用失败");
    }
}

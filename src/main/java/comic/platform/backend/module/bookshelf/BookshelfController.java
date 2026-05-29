package comic.platform.backend.module.bookshelf;

import com.baomidou.mybatisplus.core.metadata.IPage;
import comic.platform.backend.core.RestBean;
import comic.platform.backend.module.bookshelf.group.BookshelfGroup;
import comic.platform.backend.util.SecurityUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookshelf")
@Validated
public class BookshelfController {

    @Resource
    private BookshelfService bookshelfService;


    // ================== 书架核心资源 (漫画) ==================

    /**
     * 获取书架列表 (搜索/分组过滤) - 已支持分页
     */
    @GetMapping
    public RestBean<IPage<Bookshelf>> listComics(
            @RequestParam(value = "groupId", required = false) Integer groupId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        IPage<Bookshelf> bookshelfPage = bookshelfService.listBookshelf(
                SecurityUtils.getUserId(), groupId, keyword, page, size
        );
        return RestBean.success(bookshelfPage);
    }

    /**
     * 添加漫画进书架
     */
    @PostMapping
    public RestBean<String> addComic(@RequestBody @Valid Bookshelf bookshelf) {
        bookshelf.setUserId(SecurityUtils.getUserId());
        bookshelfService.addComic(bookshelf);
        return RestBean.success("添加成功");
    }

    /**
     * 移除漫画
     */
    @DeleteMapping("/{id}")
    public RestBean<String> removeComic(@PathVariable Long id) {
        bookshelfService.removeById(id);
        return RestBean.success("移除成功");
    }

    // ================== 书架周边属性 (进度与移动) ==================

    /**
     * 保存阅读进度
     */
    @PutMapping("/{id}/progress")
    public RestBean<String> updateProgress(@PathVariable Long id, @RequestBody Bookshelf progressData) {
        bookshelfService.updateProgress(SecurityUtils.getUserId(), id, progressData);
        return RestBean.success("进度保存成功");
    }

    /**
     * 移动漫画至指定分组
     */
    @PutMapping("/{id}/group")
    public RestBean<String> moveComicToGroup(@PathVariable Long id, @RequestParam("groupId") Integer groupId) {
        Bookshelf updateData = new Bookshelf();
        updateData.setId(id);
        updateData.setGroupId(groupId);
        bookshelfService.updateById(updateData);
        return RestBean.success("移动成功");
    }

    // ================== 分组管理 (Group) ==================

    /**
     * 获取所有分组
     */
    @GetMapping("/group")
    public RestBean<List<BookshelfGroup>> listGroups() {
        return RestBean.success(bookshelfService.listGroups(SecurityUtils.getUserId()));
    }

    /**
     * 新建分组
     */
    @PostMapping("/group")
    public RestBean<String> createGroup(@RequestBody @Valid BookshelfGroup group) {
        group.setUserId(SecurityUtils.getUserId());
        bookshelfService.createGroup(group);
        return RestBean.success("分组创建成功");
    }

    /**
     * 更新分组 (包括重命名)
     */
    @PutMapping("/group")
    public RestBean<String> updateGroup(@RequestBody @Valid BookshelfGroup group) {
        bookshelfService.updateGroup(SecurityUtils.getUserId(), group);
        return RestBean.success("分组更新成功");
    }

    /**
     * 供拖拽改变分组排序，批量更新排序
     */
    @PutMapping("/group/sort")
    public RestBean<String> updateGroupSort(@RequestBody @Valid List<BookshelfGroup> groupList) {
        bookshelfService.updateGroupSort(SecurityUtils.getUserId(), groupList);
        return RestBean.success("排序保存成功");
    }

    /**
     * 删除分组，漫画移至默认分组
     */
    @DeleteMapping("/group/{id}")
    public RestBean<String> deleteGroup(@PathVariable Integer id) {
        bookshelfService.deleteGroupAndMoveComics(SecurityUtils.getUserId(), id);
        return RestBean.success("分组删除成功，漫画已移至默认分组");
    }
}
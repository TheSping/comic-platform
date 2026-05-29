package comic.platform.backend.module.bookshelf;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import comic.platform.backend.module.bookshelf.group.BookshelfGroup;

import java.util.List;

public interface BookshelfService extends IService<Bookshelf> {

    // 加入书架（处理防重复逻辑）
    boolean addComic(Bookshelf bookshelf);

    // 获取书架内漫画
    IPage<Bookshelf> listBookshelf(Integer userId, Integer groupId, String keyword, int page, int size);

    // 创建分组
    boolean createGroup(BookshelfGroup group);

    // 获取当前用户的所有自定义分组
    List<BookshelfGroup> listGroups(Integer userId);

    // 更新单个分组 (改名、改状态等)
    boolean updateGroup(Integer userId, BookshelfGroup group);

    // 批量更新分组排序 (专供拖拽排序使用)
    boolean updateGroupSort(Integer userId, List<BookshelfGroup> groupList);

    // 删除分组，并将该分组下的漫画移到默认分组（groupId = 0）
    boolean deleteGroupAndMoveComics(Integer userId, Integer groupId);

    // 更新阅读进度
    boolean updateProgress(Integer userId, Long bookshelfId, Bookshelf progressData);

    // 调用图片页时，自动更新进度
    void autoSaveProgress(Integer userId, Integer sourceId, String detailUrl, String chapterName, String chapterUrl);
}

package comic.platform.backend.module.bookshelf;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import comic.platform.backend.module.bookshelf.group.BookshelfGroup;
import comic.platform.backend.module.bookshelf.group.BookshelfGroupMapper;
import comic.platform.backend.module.bookshelf.group.BookshelfGroupService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BookshelfServiceImpl extends ServiceImpl<BookshelfMapper, Bookshelf> implements BookshelfService {
    @Resource
    private BookshelfGroupMapper groupMapper;
    @Resource
    private BookshelfGroupService bookshelfGroupService;

    @Override
    public boolean addComic(Bookshelf bookshelf) {
        // 防止同一个用户重复收藏同一本书（根据 detailUrl 判断）
        LambdaQueryWrapper<Bookshelf> query = new LambdaQueryWrapper<>();
        query.eq(Bookshelf::getUserId, bookshelf.getUserId())
                .eq(Bookshelf::getDetailUrl, bookshelf.getDetailUrl());
        //SELECT COUNT(*) FROM user_bookshelf WHERE user_id = ? AND detail_url = ?;
        if (this.count(query) > 0) {
            throw new RuntimeException("该漫画已在书架中，请勿重复添加");
        }

        return this.save(bookshelf);
    }

    @Override
    public List<Bookshelf> listBookshelf(Integer userId, Integer groupId, String keyword) {
        LambdaQueryWrapper<Bookshelf> query = new LambdaQueryWrapper<>();
        query.eq(Bookshelf::getUserId, userId)
                .eq(groupId != null, Bookshelf::getGroupId, groupId)
                .like(keyword != null && !keyword.trim().isEmpty(), Bookshelf::getComicName, keyword)
                .orderByDesc(Bookshelf::getLastReadTime);
        return this.list(query);
    }

    @Override
    public boolean createGroup(BookshelfGroup group) {
        return groupMapper.insert(group) > 0;
    }

    @Override
    public boolean updateGroup(Integer userId, BookshelfGroup group) {
        group.setUserId(userId);
        return groupMapper.updateById(group) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务，保证要死一起死，不会出现部分排序错乱
    public boolean updateGroupSort(Integer userId, List<BookshelfGroup> groupList) {
        if (groupList == null || groupList.isEmpty()) {
            return true;
        }

        for (int i = 0; i < groupList.size(); i++) {
            BookshelfGroup group = groupList.get(i);
            group.setUserId(userId);
            group.setSortOrder(i + 1);
        }

        return bookshelfGroupService.updateBatchById(groupList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启数据库事务
    public boolean deleteGroupAndMoveComics(Integer userId, Integer groupId) {
        if (groupId == null || groupId == 0) {
            throw new RuntimeException("默认分组不可删除");
        }

        // 1. 将该分组下的所有漫画，转移到默认分组 (group_id = 0)
        LambdaUpdateWrapper<Bookshelf> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Bookshelf::getUserId, userId)
                .eq(Bookshelf::getGroupId, groupId)
                .set(Bookshelf::getGroupId, 0);
        //UPDATE user_bookshelf SET group_id = 0 WHERE user_id = ? AND group_id = ?
        this.update(updateWrapper);

        // 2. 删除该分组本身
        LambdaQueryWrapper<BookshelfGroup> deleteGroupWrapper = new LambdaQueryWrapper<>();
        deleteGroupWrapper.eq(BookshelfGroup::getUserId, userId)
                .eq(BookshelfGroup::getId, groupId);
        //DELETE FROM bookshelf_group WHERE user_id = ? AND groupId = ?
        return groupMapper.delete(deleteGroupWrapper) > 0;
    }

    @Override
    public boolean updateProgress(Integer userId, Long bookshelfId, Bookshelf progressData) {
        LambdaUpdateWrapper<Bookshelf> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Bookshelf::getUserId, userId)
                .eq(Bookshelf::getId, bookshelfId)
                // 设置进度参数
                .set(Bookshelf::getLastChapterName, progressData.getLastChapterName())
                .set(Bookshelf::getLastReadChapterUrl, progressData.getLastReadChapterUrl())
                .set(Bookshelf::getLastReadPageIndex, progressData.getLastReadPageIndex())
                .set(Bookshelf::getLastReadTime, new Date()); // 强行设为当前时间

        return this.update(updateWrapper);
    }

    @Override
    public List<BookshelfGroup> listGroups(Integer userId) {
        LambdaQueryWrapper<BookshelfGroup> query = new LambdaQueryWrapper<>();
        query.eq(BookshelfGroup::getUserId, userId)
                .orderByAsc(BookshelfGroup::getSortOrder);

        return groupMapper.selectList(query);
    }
}

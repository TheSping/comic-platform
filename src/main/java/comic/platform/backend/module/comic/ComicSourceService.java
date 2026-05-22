package comic.platform.backend.module.comic;

import com.baomidou.mybatisplus.extension.service.IService;
import comic.platform.backend.entity.ComicSource;

import java.util.List;

public interface ComicSourceService extends IService<ComicSource> {

    /**
     * 手动刷新缓存
     */
    void refreshCache();


    /**
     * 添加书源，并同步刷新内存缓存
     */
    boolean addSource(ComicSource source);

    /**
     * 更新书源，并同步刷新内存缓存
     */
    boolean updateSource(ComicSource source);

    /**
     * 删除书源，并同步刷新内存缓存
     */
    boolean deleteSource(Integer id);

    /**
     * 获取所有已启用的书源
     */
    List<ComicSource> getAllActiveSourcesFromCache();

    /**
     * 根据 ID 获取单个书源 (直接从内存读)
     */
    ComicSource getActiveSourceByIdFromCache(Integer id);

    /**
     * 启用指定书源
     */
    boolean enableSource(Integer id);

    /**
     * 禁用指定书源
     */
    boolean disableSource(Integer id);

    /**
     * 批量添加书源
     */
    boolean addSourcesBatch(List<ComicSource> sources);

    /**
     * 批量删除书源
     */
    boolean deleteSourcesBatch(List<Integer> ids);

    /**
     * 批量启用书源
     */
    boolean enableSourcesBatch(List<Integer> ids);

    /**
     * 批量禁用书源
     */
    boolean disableSourcesBatch(List<Integer> ids);

    /**
     * 查询所有书源列表（支持按名称模糊搜索）
     */
    List<ComicSource> queryAdminSources(String keyword);

}

package comic.platform.backend.module.comic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ComicSourceServiceImpl extends ServiceImpl<ComicSourceMapper, ComicSource> implements ComicSourceService {
    /**
     * 内存数据库
     * 使用 ConcurrentHashMap 保证高并发下的绝对线程安全
     */
    private final Map<Integer, ComicSource> sourceCache = new ConcurrentHashMap<>();

    /**
     * 系统启动后自动初始化读一次启用源
     */
    @PostConstruct
    public void init() {
        log.info("系统启动：准备加载【已启用】的漫画源到内存缓存...");
        refreshCache();
    }

    /**
     * 刷新书源，将开启的书源加进缓存
     */
    @Override
    public void refreshCache() {
        sourceCache.clear();
        List<ComicSource> activeSources = this.list(new LambdaQueryWrapper<ComicSource>().eq(ComicSource::getEnable, 1));
        for (ComicSource source : activeSources) {
            sourceCache.put(source.getId(), source);
        }

        log.info("✅ 漫画源缓存加载完成！共加载了 {} 个可用书源。", sourceCache.size());
    }

    /**
     * 增加书源、刷新
     */
    @Override
    public boolean addSource(ComicSource source) {
        boolean success = super.save(source);
        if (success) refreshCache();
        return success;
    }

    /**
     * 更新书源、刷新
     */
    @Override
    public boolean updateSource(ComicSource source) {
        boolean success = super.updateById(source);
        if (success) refreshCache();
        return success;
    }

    /**
     * 删除书源、刷新
     */
    @Override
    public boolean deleteSource(Integer id) {
        boolean success = super.removeById(id);
        if (success) refreshCache();
        return success;
    }

    /**
     * 获取启用的全部书源
     */
    @Override
    public List<ComicSource> getAllActiveSourcesFromCache() {
        // 返回 values 的一个新 ArrayList，防止外部代码拿到引用后乱改，保护缓存的安全
        return new ArrayList<>(sourceCache.values());
    }

    /**
     * 根据 ID 获取单个书源 (直接从内存读)
     */
    @Override
    public ComicSource getActiveSourceByIdFromCache(Integer id) {
        return sourceCache.get(id);
    }

    /**
     * 启用书源
     */
    @Override
    public boolean enableSource(Integer id) {
        boolean success = this.update(
                new LambdaUpdateWrapper<ComicSource>()
                        .eq(ComicSource::getId, id)
                        .set(ComicSource::getEnable, 1)
        );

        if (success) {
            refreshCache();
            log.info("书源 [ID: {}] 已成功启用，内存缓存已同步。", id);
        }
        return success;
    }

    /**
     * 禁用书源
     */
    @Override
    public boolean disableSource(Integer id) {
        boolean success = this.update(
                new LambdaUpdateWrapper<ComicSource>()
                        .eq(ComicSource::getId, id)
                        .set(ComicSource::getEnable, 0)
        );

        if (success) {
            refreshCache();
            log.info("书源 [ID: {}] 已成功禁用，内存缓存已同步。", id);
        }
        return success;
    }

    @Override
    public boolean addSourcesBatch(List<ComicSource> sources) {
        if (sources == null || sources.isEmpty()) return false;

        boolean success = super.saveBatch(sources);
        if (success) {
            refreshCache();
            log.info("批量导入 {} 个书源成功，缓存已同步！", sources.size());
        }
        return success;
    }

    @Override
    public boolean deleteSourcesBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return false;

        // DELETE FROM comic_source WHERE id IN (1, 2, 3...)
        boolean success = super.removeByIds(ids);
        if (success) {
            refreshCache();
            log.info("批量删除 {} 个书源成功，缓存已同步！", ids.size());
        }
        return success;
    }

    @Override
    public boolean enableSourcesBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return false;

        // UPDATE comic_source SET enable = 1 WHERE id IN (1, 2, 3...)
        boolean success = this.update(new LambdaUpdateWrapper<ComicSource>()
                .in(ComicSource::getId, ids)
                .set(ComicSource::getEnable, 1));

        if (success) {
            refreshCache();
            log.info("批量启用 {} 个书源成功，缓存已同步！", ids.size());
        }
        return success;
    }

    @Override
    public boolean disableSourcesBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return false;

        boolean success = this.update(new LambdaUpdateWrapper<ComicSource>()
                .in(ComicSource::getId, ids)
                .set(ComicSource::getEnable, 0));

        if (success) {
            refreshCache();
            log.info("批量禁用 {} 个书源成功，缓存已同步！", ids.size());
        }
        return success;
    }

    @Override
    public List<ComicSource> queryAdminSources(String keyword) {
        LambdaQueryWrapper<ComicSource> wrapper = new LambdaQueryWrapper<>();

        // 如果前端传了搜索词，就拼上 LIKE 条件
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ComicSource::getSourceName, keyword);
        }

        // 先按照 enable 降序（1排前面，0排后面），再按照 ID 降序（最新添加的排前面）
        wrapper.orderByDesc(ComicSource::getEnable)
                .orderByDesc(ComicSource::getId);

        return this.list(wrapper);
    }


}

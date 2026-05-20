package comic.platform.backend.module.comic;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import comic.platform.backend.entity.ComicSource;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ComicSourceMapper extends BaseMapper<ComicSource> {
}

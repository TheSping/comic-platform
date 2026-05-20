package comic.platform.backend.module.comic;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import comic.platform.backend.entity.ComicSource;
import org.springframework.stereotype.Service;

@Service
// 继承 ServiceImpl，MyBatis-Plus 会自动把 Mapper 注入进来并实现基础 CRUD
public class ComicSourceServiceImpl extends ServiceImpl<ComicSourceMapper, ComicSource> implements ComicSourceService {
}

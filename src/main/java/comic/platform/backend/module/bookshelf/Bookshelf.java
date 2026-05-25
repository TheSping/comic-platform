package comic.platform.backend.module.bookshelf;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户书架漫画表
 */
@Data
@TableName("user_bookshelf")
public class Bookshelf {

    /**
     * 书架主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID (关联 c_user.id)
     */
    private Integer userId;

    /**
     * 漫画名称
     */
    private String comicName;

    /**
     * 封面URL
     */
    private String comicCover;

    /**
     * 作者
     */
    private String comicAuthor;

    /**
     * 漫画详情页URL (核心定位)
     */
    private String detailUrl;

    /**
     * 书源ID
     */
    private Integer sourceId;

    /**
     * 最后阅读的章节名
     */
    private String lastChapterName;

    /**
     * 最后阅读的章节URL
     */
    private String lastReadChapterUrl;

    /**
     * 最后阅读的页码索引
     */
    private Integer lastReadPageIndex;

    /**
     * 最后阅读时间
     */
    private LocalDateTime lastReadTime;

    /**
     * 分组ID (0=默认分组)
     */
    private Integer groupId = 0;

    /**
     * 加入书架时间
     */
    private LocalDateTime createdAt;
}

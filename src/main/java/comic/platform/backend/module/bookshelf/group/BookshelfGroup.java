package comic.platform.backend.module.bookshelf.group;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户书架分组表
 */
@Data
@TableName("bookshelf_group")
public class BookshelfGroup {

    /**
     * 分组主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 所属用户ID
     */
    private Integer userId;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 排序权重
     */
    private Integer sortOrder;
}
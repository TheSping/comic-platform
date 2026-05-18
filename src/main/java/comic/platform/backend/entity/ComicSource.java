package comic.platform.backend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

@Data
@TableName(value = "comic_source", autoResultMap = true)// 开启复杂的类型映射
public class ComicSource {

    private Long id;
    private String sourceName;
    private String sourceUrl;

    // ================= 重点来了 =================
    // 把数据库里的 JSON 字符串，用 Jackson 自动转换成对象
    @TableField(typeHandler = JacksonTypeHandler.class)
    private RuleSearch ruleSearch;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private RuleToc ruleToc;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private RuleContent ruleContent;

    private Integer enable;

    // ================= 定义内部类 (定义JSON里的结构) =================

    @Data
    public static class RuleSearch {
        private String searchUrl;    // 搜索URL模板，如 /search?keyword={{key}}
        private String list;         // 列表规则，如 .search-list .item
        private String name;         // 书名规则，如 a.title@text
        private String author;       // 作者规则
        private String cover;        // 封面规则
        private String detailUrl;    // 详情页链接规则
    }

    @Data
    public static class RuleToc {
        private String list;         // 目录列表规则，如 .chapter-list li
        private String name;         // 章节名称规则，如 a@text
        private String url;          // 章节链接规则，如 a@href
    }

    @Data
    public static class RuleContent {
        private String image;        // 漫画图片规则，如 .comic-page img@src
    }
}
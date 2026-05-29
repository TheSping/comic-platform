package comic.platform.backend.module.comic;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@TableName(value = "comic_source", autoResultMap = true)
@Schema(description = "图源配置实体类")
public class ComicSource {

    @Schema(description = "主键ID")
    private Integer id;

    @Schema(description = "图源名称", example = "包子漫画")
    @NotBlank(message = "书源名称不能为空")
    private String sourceName;

    @Schema(description = "图源官网地址", example = "https://www.baozimh.com")
    @NotBlank(message = "书源名称不能为空")
    private String sourceUrl;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "搜索规则配置")
    private RuleSearch ruleSearch;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "目录规则配置")
    private RuleToc ruleToc;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "正文图片规则配置")
    private RuleContent ruleContent;

    @Schema(description = "是否启用状态 (1:启用, 0:停用)", example = "1")
    private Integer enable;

    // ================= 内部类 =================

    @Data
    @Schema(description = "搜索规则定义 (支持语法：CSS选择器、$.JSONPath、//XPath、:regex)")
    public static class RuleSearch {
        @Schema(description = "搜索请求URL模板，使用 {{key}} 作为占位符", example = "/search?keyword={{key}}")
        private String searchUrl;

        @Schema(description = "漫画列表的定位规则", example = ".cont-list li")
        private String list;

        @Schema(description = "漫画书名的提取规则", example = "a.txtA@text")
        private String name;

        @Schema(description = "原作作者的提取规则", example = "p.author@text")
        private String author;

        @Schema(description = "漫画封面的提取规则", example = "img@src")
        private String cover;

        @Schema(description = "详情页或目录页的链接提取规则", example = "a.txtA@href")
        private String detailUrl;

        @Schema(description = "下一页链接规则", example = "a.next@href")
        private String nextPage;
    }

    @Data
    @Schema(description = "目录章节规则定义")
    public static class RuleToc {
        @Schema(description = "章节列表的定位规则", example = ".chapter-list li")
        private String list;

        @Schema(description = "章节名称的提取规则", example = "a@text")
        private String name;

        @Schema(description = "章节链接的提取规则", example = "a@href")
        private String url;

        @Schema(description = "下一页链接规则", example = "a.next-page@href")
        private String nextPage;
    }

    @Data
    @Schema(description = "正文图片规则定义")
    public static class RuleContent {
        @Schema(description = "漫画图片的提取规则", example = ".comic-page img@src")
        private String image;
    }
}
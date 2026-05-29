package comic.platform.backend.module.comic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "聚合搜索返回结果 (按书源分组)")
public class SearchResult {
    @Schema(description = "书源ID", example = "1")
    private Integer sourceId;

    @Schema(description = "书源名称", example = "包子漫画")
    private String sourceName;

    @Schema(description = "搜索出的漫画列表",
            example = "[{\"name\": \"海贼王\", \"cover\": \"https://xxx.jpg\", \"url\": \"/comic/123\"}]")
    private List<Map<String, String>> comics;

    @Schema(description = "该书源搜索结果的下一页URL (若无则为null)",
            example = "https://baozimh.com/search?page=2")
    private String nextPageUrl;
}
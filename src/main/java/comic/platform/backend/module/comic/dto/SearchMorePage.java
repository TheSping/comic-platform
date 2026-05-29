package comic.platform.backend.module.comic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "前端提交的批量翻页请求体")
public class SearchMorePage {
    @Schema(description = "要翻页的书源ID", example = "1")
    private Integer sourceId;

    @Schema(description = "该书源的下一页链接", example = "https://baozimh.com/search?page=2")
    private String nextPageUrl;
}

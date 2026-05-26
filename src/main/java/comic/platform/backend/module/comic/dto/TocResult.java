package comic.platform.backend.module.comic.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TocResult {
    // 当前页解析出来的章节列表
    private List<Map<String, String>> chapters;

    // 解析出来的下一页链接（如果没有下一页了，就是 null 或 ""）
    private String nextPageUrl;
}
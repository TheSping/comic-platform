package comic.platform.backend.module.comic.parser.Filter;

import org.springframework.stereotype.Component;

/**
 * 去 HTML 标签
 */
@Component
public class RemoveTagsFilter implements StringFilter {
    @Override
    public String name() {
        return "removeTags";
    }

    @Override
    public String filter(String input, String param) {
        if (input == null) return "";
        // 正则剥离所有以 < 开头、> 结尾的标签
        return input.replaceAll("<[^>]*>", "");
    }
}
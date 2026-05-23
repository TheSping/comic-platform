package comic.platform.backend.module.comic.parser.Filter;

import org.springframework.stereotype.Component;

/**
 * 正则替换
 */
@Component
public class ReplaceFilter implements StringFilter {
    @Override
    public String name() {
        return "replace";
    }

    @Override
    public String filter(String input, String param) {
        if (input == null || param == null || param.isEmpty()) return input != null ? input : "";

        // 约定格式为：正则,替换目标 (例如 thumb.jpg,large.jpg)
        // 用 limit=2 确保只切分第一个逗号，防止替换词里带有逗号
        String[] parts = param.split(",", 2);

        String regex = parts[0];
        String replacement = parts.length > 1 ? parts[1] : ""; // 如果只有正则没有逗号，则默认替换为空字符串

        try {
            return input.replaceAll(regex, replacement);
        } catch (Exception e) {
            // 防止正则写错导致程序崩溃
            return input;
        }
    }
}
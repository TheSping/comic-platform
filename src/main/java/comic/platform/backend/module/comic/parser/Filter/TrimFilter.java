package comic.platform.backend.module.comic.parser.Filter;

import org.springframework.stereotype.Component;

/**
 * 去首尾空白
 */
@Component
public class TrimFilter implements StringFilter {
    @Override
    public String name() {
        return "trim";
    }

    @Override
    public String filter(String input, String param) {
        if (input == null) return "";
        return input.trim();
    }
}
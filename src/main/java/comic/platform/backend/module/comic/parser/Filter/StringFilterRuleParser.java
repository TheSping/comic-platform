package comic.platform.backend.module.comic.parser.Filter;

import comic.platform.backend.module.comic.parser.RuleParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StringFilterRuleParser implements RuleParser {

    // Spring 会自动把所有实现了 StringFilter 的 Bean 注入进来
    private final List<StringFilter> filters;

    @Autowired
    public StringFilterRuleParser(List<StringFilter> filters) {
        this.filters = filters;
    }

    @Override
    public boolean match(String rule) {
        // 匹配 @trim, @replace:, @removeTags 等 @xxx 格式规则
        return rule != null && rule.startsWith("@");
    }

    @Override
    public String parse(String sourceData, String rule) {
        if (sourceData == null || rule == null) return "";

        for (StringFilter filter : filters) {
            String prefix = "@" + filter.name();
            // 如果规则匹配了当前的过滤器（比如 rule 是 "@replace:small,large"）
            if (rule.startsWith(prefix)) {
                String param = "";
                // 提取冒号后面的参数
                if (rule.length() > prefix.length() && rule.charAt(prefix.length()) == ':') {
                    param = rule.substring(prefix.length() + 1);
                }
                return filter.filter(sourceData, param);
            }
        }

        // 如果是 @xxx 但没找到对应的 filter，原样返回
        return sourceData;
    }

    @Override
    public List<String> parseList(String sourceData, String rule) {
        // 字符串过滤器通常只处理单条数据。如果在获取 List 的环节用到了，我们直接包一层返回
        List<String> list = new ArrayList<>();
        list.add(parse(sourceData, rule));
        return list;
    }
}

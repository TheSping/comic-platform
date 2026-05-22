package comic.platform.backend.module.comic.parser.Impl;


import comic.platform.backend.module.comic.parser.RuleParser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegexRuleParser implements RuleParser {

    @Override
    public boolean match(String rule) {
        // 匹配 Legado 的语法：以 : 或 $0,$1 开头
        return rule != null && (rule.startsWith(":") || rule.startsWith("$0,$1"));
    }

    @Override
    public String parse(String sourceData, String rule) {

        // 清洗前缀，拿到纯正的正则表达式
        // 例如规则是：  :var picUrl = "(.*?)";
        String regex = rule.substring(1);

        // 编译正则并匹配
        Matcher matcher = Pattern.compile(regex).matcher(sourceData);

        if (matcher.find()) {
            // 如果用户在正则里写了括号 ()，说明他想提取括号里的内容（捕获组 1）
            if (matcher.groupCount() >= 1) {
                return matcher.group(1).trim();
            }
            // 否则返回整个匹配到的字符串
            return matcher.group().trim();
        }

        return "";
    }

    @Override
    public List<String> parseList(String sourceData, String rule) {
        List<String> resultList = new ArrayList<>();
        if (rule == null || rule.isEmpty() || sourceData == null) return resultList;

        String regex = rule.substring(1);
        Matcher matcher = Pattern.compile(regex).matcher(sourceData);

        while (matcher.find()) {
            String value;
            if (matcher.groupCount() >= 1) {
                value = matcher.group(1).trim(); // 优先提取括号内的捕获组
            } else {
                value = matcher.group().trim();  // 提取全部
            }

            if (!value.isEmpty()) {
                resultList.add(value);
            }
        }

        return resultList;
    }
}
package comic.platform.backend.module.comic.parser.Impl;

import comic.platform.backend.module.comic.parser.RuleParser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegexRuleParser implements RuleParser {

    // 用于拆分 Legado 前缀和真实正则表达式。
    // 匹配前缀格式，例如: $1, $0,$1, $1-$2 等。限制分隔符为常见标点符号，防止吞掉后面的正则。
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^((\\$\\d+[,\\-=\\s_~|:/]*)+)(.*)");

    // 用于在格式化拼接时提取 $数字
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$(\\d+)");

    /**
     * 内部类，用于存放拆分后的规则信息
     */
    private static class ParsedRule {
        String format;      // 提取出的格式化前缀，如 "$0,$1"
        String regex;       // 提取出的纯正则表达式，如 "<h1>(.+?)</h1>"
        boolean hasFormat;  // 是否包含自定义格式拼接
    }

    @Override
    public boolean match(String rule) {
        // 匹配 Legado 的语法：以 : 开头，或者以 $数字 开头（覆盖 $1, $0,$1 等）
        return rule != null && (rule.startsWith(":") || rule.matches("^\\$\\d+.*"));
    }

    /**
     * 解析并分离 格式化语法 和 纯正则表达式
     */
    private ParsedRule parseRuleString(String rule) {
        ParsedRule pr = new ParsedRule();
        if (rule.startsWith(":")) {
            pr.regex = rule.substring(1);
            pr.hasFormat = false;
        } else if (rule.startsWith("$")) {
            Matcher m = PREFIX_PATTERN.matcher(rule);
            if (m.find()) {
                pr.format = m.group(1); // 提取出来的格式，例如 $0,$1
                pr.regex = m.group(3);  // 剩下的就是纯正则
                pr.hasFormat = true;
            } else {
                // 如果正则拆分兜底失败，做降级处理
                pr.regex = rule.substring(1);
                pr.hasFormat = false;
            }
        } else {
            pr.regex = rule;
            pr.hasFormat = false;
        }
        return pr;
    }

    /**
     * 根据格式化字符串拼接结果（如 "$0,$1" 替换为 "匹配总文本,捕获组1"）
     */
    private String formatMatch(Matcher matcher, String format) {
        Matcher fMatcher = VAR_PATTERN.matcher(format);
        StringBuffer sb = new StringBuffer();

        while (fMatcher.find()) {
            // 获取 $ 后面的数字
            int groupIndex = Integer.parseInt(fMatcher.group(1));
            String groupValue = "";

            // 确保数字不越界
            if (groupIndex <= matcher.groupCount()) {
                groupValue = matcher.group(groupIndex);
                if (groupValue == null) {
                    groupValue = "";
                }
            }
            // 使用 Matcher.quoteReplacement 防止 groupValue 中含有特殊字符（如 $ 或 \）导致报错
            fMatcher.appendReplacement(sb, Matcher.quoteReplacement(groupValue));
        }
        fMatcher.appendTail(sb);
        return sb.toString().trim();
    }

    @Override
    public String parse(String sourceData, String rule) {
        if (rule == null || rule.isEmpty() || sourceData == null) return "";

        // 1. 解析规则，拿到 前缀格式 和 纯正则
        ParsedRule parsedRule = parseRuleString(rule);

        // 2. 编译并匹配
        Matcher matcher = Pattern.compile(parsedRule.regex).matcher(sourceData);

        if (matcher.find()) {
            // 如果含有 $0,$1 这种格式化语法，进行拼接替换
            if (parsedRule.hasFormat) {
                return formatMatch(matcher, parsedRule.format);
            }
            // 否则兼容原逻辑：有括号提括号，没括号提全部
            else {
                if (matcher.groupCount() >= 1) {
                    return matcher.group(1).trim();
                }
                return matcher.group().trim();
            }
        }

        return "";
    }

    @Override
    public List<String> parseList(String sourceData, String rule) {
        List<String> resultList = new ArrayList<>();
        if (rule == null || rule.isEmpty() || sourceData == null) return resultList;

        ParsedRule parsedRule = parseRuleString(rule);
        Matcher matcher = Pattern.compile(parsedRule.regex).matcher(sourceData);

        while (matcher.find()) {
            String value;

            if (parsedRule.hasFormat) {
                value = formatMatch(matcher, parsedRule.format);
            } else {
                if (matcher.groupCount() >= 1) {
                    value = matcher.group(1).trim();
                } else {
                    value = matcher.group().trim();
                }
            }

            if (!value.isEmpty()) {
                resultList.add(value);
            }
        }

        return resultList;
    }
}
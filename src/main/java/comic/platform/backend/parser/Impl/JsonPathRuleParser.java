package comic.platform.backend.parser.Impl;

import comic.platform.backend.parser.RuleParser;
import org.springframework.stereotype.Component;

@Component
public class JsonPathRuleParser implements RuleParser {
    //@Json: 或 $. / $[	JSONPath 表达式	$.data.book.title

    @Override
    public boolean match(String rule) {
        // 按照文档 1.2 的规范：以 $. 或 $[ 开头的
        return rule != null && (rule.startsWith("$.") || rule.startsWith("$[") || rule.startsWith("@Json:"));
    }

    @Override
    public String parse(String sourceData, String rule) {
        try {
            // 使用 JsonPath 解析 JSON 字符串
            // 例如 rule 是 "$.data.bookList[0].name"
            Object result = com.jayway.jsonpath.JsonPath.read(sourceData, rule);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            return ""; // 解析失败容错
        }
    }
}

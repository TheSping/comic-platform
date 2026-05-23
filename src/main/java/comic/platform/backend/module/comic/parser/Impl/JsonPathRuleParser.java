package comic.platform.backend.module.comic.parser.Impl;

import com.jayway.jsonpath.JsonPath;
import comic.platform.backend.module.comic.parser.RuleParser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JsonPathRuleParser implements RuleParser {
    //@Json: 或 $. / $[  JSONPath 表达式   $.data.book.title

    @Override
    public boolean match(String rule) {
        return rule != null && (rule.startsWith("$.") || rule.startsWith("$[") || rule.startsWith("@Json:"));
    }

    @Override
    public String parse(String sourceData, String rule) {
        // 如果以 @Json: 开头，截掉前 6 个字符，提取纯正的 JsonPath
        if (rule != null && rule.startsWith("@Json:")) {
            rule = rule.substring(6);
        }

        // 使用 JsonPath 解析 JSON 字符串
        // 例如 rule 是 "$.data.bookList[0].name"
        Object result = JsonPath.read(sourceData, rule);
        return result != null ? result.toString() : "";
    }

    @Override
    public List<String> parseList(String sourceData, String rule) {
        List<String> resultList = new ArrayList<>();
        if (rule == null || rule.isEmpty() || sourceData == null) return resultList;

        if (rule.startsWith("@Json:")) {
            rule = rule.substring(6);
        }

        Object result = JsonPath.read(sourceData, rule);

        if (result != null) {
            // 如果 JsonPath 提取出来的是一个 JSON 数组
            if (result instanceof List) {
                for (Object obj : (List<?>) result) {
                    if (obj != null) {
                        resultList.add(obj.toString().trim());
                    }
                }
            } else {
                // 如果提取出来只有单个字符串，也包装成 List 返回
                resultList.add(result.toString().trim());
            }
        }

        return resultList;
    }
}
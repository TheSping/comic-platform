package comic.platform.backend.module.comic.parser.Impl;

import comic.platform.backend.module.comic.parser.RuleParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JsoupRuleParser implements RuleParser {
    // div#content@text

    @Override
    public boolean match(String rule) {
        // 因为它是最常用的，所以只要别人都不管，就默认归Jsoup管
        return rule != null && !rule.startsWith("$.") && !rule.startsWith("@XPath:");
    }

    @Override
    public String parse(String sourceData, String rule) {
        // 切割规则
        String[] parts = rule.split("@");
        String cssQuery = parts[0];
        String attr = parts.length > 1 ? parts[1] : "text";

        // 定位元素
        Element target = Jsoup.parse(sourceData).selectFirst(cssQuery);

        if (target == null) return "";

        // 提取
        if ("text".equalsIgnoreCase(attr)) {
            return target.text().trim();
        } else {
            return target.attr(attr).trim();
        }
    }

    @Override
    public List<String> parseList(String sourceData, String rule) {
        List<String> result = new ArrayList<>();
        if (rule == null || rule.trim().isEmpty() || sourceData == null) return result;

        String[] parts = rule.split("@");
        String cssQuery = parts[0];
        String attr = parts.length > 1 ? parts[1] : "text";

        Elements elements = Jsoup.parse(sourceData).select(cssQuery);

        for (Element target : elements) {
            String value = "";
            if ("text".equalsIgnoreCase(attr)) {
                value = target.text().trim();
            } else if ("html".equalsIgnoreCase(attr)) {
                value = target.html();
            } else {
                value = target.attr(attr).trim();
            }

            if (!value.isEmpty()) {
                result.add(value);
            }
        }

        return result;
    }

}

package comic.platform.backend.parser.Impl;

import comic.platform.backend.parser.RuleParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class XPathRuleParser implements RuleParser {

    @Override
    public boolean match(String rule) {
        // 匹配 Legado 的语法：以 @XPath: 或者 // 开头
        return rule != null && (rule.startsWith("@XPath:") || rule.startsWith("//"));
    }

    @Override
    public String parse(String sourceData, String rule) {
        try {
            // 清洗前缀，拿到纯正的 XPath 表达式
            String xpath = rule.startsWith("@XPath:") ? rule.substring(7) : rule;
            Document doc = Jsoup.parse(sourceData);

            // Jsoup 原生支持的 XPath 查询
            Elements elements = doc.selectXpath(xpath);
            if (elements.isEmpty()) {
                return "";
            }

            // 处理 XPath 特有的属性提取逻辑 (例如 //img/@src)
            // 如果 XPath 本身是指向一个属性，Jsoup 取出来后可以直接拿
            if (xpath.contains("/@")) {
                String attrName = xpath.substring(xpath.lastIndexOf("/@") + 2);
                return elements.get(0).attr(attrName).trim();
            }

            // 否则默认提取文本
            return elements.get(0).text().trim();

        } catch (Exception e) {
            return ""; // 解析容错
        }
    }

    @Override
    public List<String> parseList(String sourceData, String rule) {
        List<String> resultList = new ArrayList<>();
        if (rule == null || rule.isEmpty() || sourceData == null) return resultList;

        try {
            String xpath = rule.startsWith("@XPath:") ? rule.substring(7) : rule;
            Document doc = Jsoup.parse(sourceData);
            Elements elements = doc.selectXpath(xpath);

            for (Element el : elements) {
                String value = "";
                // 判断 XPath 是否需要提取属性 (例如 //img/@src)
                if (xpath.contains("/@")) {
                    String attrName = xpath.substring(xpath.lastIndexOf("/@") + 2);
                    value = el.attr(attrName).trim();
                } else {
                    value = el.text().trim();
                }

                if (!value.isEmpty()) {
                    resultList.add(value);
                }
            }
        } catch (Exception e) {
            // 容错处理
        }
        return resultList;
    }
}

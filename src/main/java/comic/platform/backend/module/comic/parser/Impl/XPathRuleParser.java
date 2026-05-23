package comic.platform.backend.module.comic.parser.Impl;

import comic.platform.backend.module.comic.parser.RuleParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class XPathRuleParser implements RuleParser {

    // 严谨匹配末尾的 /@属性名 (支持 xlink:href 这种带冒号的属性)
    // $ 表示必须在字符串结尾，防止误伤中间的选择器语法
    private static final Pattern ATTR_PATTERN = Pattern.compile("/@([a-zA-Z0-9_\\-:]+)$");

    @Override
    public boolean match(String rule) {
        // 匹配 Legado 的语法：以 @XPath: 或者 // 开头
        return rule != null && (rule.startsWith("@XPath:") || rule.startsWith("//"));
    }

    @Override
    public String parse(String sourceData, String rule) {
        if (rule == null || rule.isEmpty() || sourceData == null) return "";

        // 清洗前缀，拿到纯正的 XPath 表达式并去掉两端多余的空格
        String xpath = rule.startsWith("@XPath:") ? rule.substring(7).trim() : rule.trim();

        // 提前用正则判断是否是属性提取，并提取出属性名
        Matcher matcher = ATTR_PATTERN.matcher(xpath);
        boolean isAttr = matcher.find();
        String attrName = isAttr ? matcher.group(1) : null;

        // Jsoup 原生支持的 XPath 查询
        Elements elements = Jsoup.parse(sourceData).selectXpath(xpath);
        if (elements.isEmpty()) {
            return "";
        }

        // 如果明确是提取属性，返回属性值
        if (isAttr) {
            return elements.get(0).attr(attrName).trim();
        }

        // 否则默认提取文本
        return elements.get(0).text().trim();
    }

    @Override
    public List<String> parseList(String sourceData, String rule) {
        List<String> resultList = new ArrayList<>();
        if (rule == null || rule.isEmpty() || sourceData == null) return resultList;

        String xpath = rule.startsWith("@XPath:") ? rule.substring(7).trim() : rule.trim();

        // 解析前就把是不是属性提炼出来，避免在 for 循环里重复做正则匹配
        Matcher matcher = ATTR_PATTERN.matcher(xpath);
        boolean isAttr = matcher.find();
        String attrName = isAttr ? matcher.group(1) : null;

        Elements elements = Jsoup.parse(sourceData).selectXpath(xpath);

        for (Element el : elements) {
            String value;
            // 按照前面的正则分析结果进行提取
            if (isAttr) {
                value = el.attr(attrName).trim();
            } else {
                value = el.text().trim();
            }

            if (!value.isEmpty()) {
                resultList.add(value);
            }
        }

        return resultList;
    }
}
package comic.platform.backend.module.comic.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import comic.platform.backend.entity.ComicSource;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ParserEngine {

    // 自动收集所有实现了 RuleParser 接口的 Bean
    private final List<RuleParser> parsers;

    // Spring 自带的 JSON 神器
    private final ObjectMapper objectMapper = new ObjectMapper();

    //解析
    public String executeParse(String sourceData,
                               String rule) {
        if (rule == null || rule.isEmpty()) return "";

        // 遍历所有解析器
        for (RuleParser parser : parsers) {
            // 只要有一个实现说true，就把数据丢给它去处理
            if (parser.match(rule)) {
                return parser.parse(sourceData, rule);
            }
        }

        return "无法识别的规则语法";
    }

    /**
     * 列表切割器 (ListCutter)
     * 将 HTML || JSON，切成小块的 String 列表
     */
    private List<String> splitByRule(String sourceData,
                                     String listRule) {
        List<String> result = new ArrayList<>();
        if (listRule == null || listRule.isEmpty()) return result;

        try {
            if (listRule.startsWith("$.")) {
                // 1. JSON 切割模式：用 JsonPath 读出一个集合
                List<Object> list = JsonPath.read(sourceData, listRule);
                for (Object obj : list) {
                    // 把集合里的每个小对象，重新序列化成 JSON 字符串，喂给后面的解析器
                    result.add(objectMapper.writeValueAsString(obj));
                }
            } else {
                // 2. HTML 切割模式：用 Jsoup 切割 DOM 树
                Document doc = Jsoup.parse(sourceData);
                Elements elements = doc.select(listRule);
                for (Element el : elements) {
                    // 把每个 DOM 节点变回 HTML 字符串
                    result.add(el.outerHtml());
                }
            }
        } catch (Exception e) {
            // 容错处理
        }
        return result;
    }

    /**
     * 解析搜索列表 (Search)
     */
    public List<Map<String, String>> parseSearchList(String sourceData,
                                                     ComicSource.RuleSearch rule) {
        List<Map<String, String>> resultList = new ArrayList<>();
        // 将搜索出来的列表切成单个漫画
        List<String> items = splitByRule(sourceData, rule.getList());

        //将每个漫画的name、cover、detailUrl拿出来
        for (String itemData : items) {
            Map<String, String> data = new HashMap<>();
            data.put("name", executeParse(itemData, rule.getName()));
            data.put("cover", executeParse(itemData, rule.getCover()));
            data.put("detailUrl", executeParse(itemData, rule.getDetailUrl()));
            resultList.add(data);
        }
        return resultList;
    }

    /**
     * 解析目录列表 (Toc)
     * 逻辑和搜索列表一模一样，只是提取的字段变成了 章节名 和 章节链接
     */
    public List<Map<String, String>> parseTocList(String sourceData,
                                                  ComicSource.RuleToc rule) {
        List<Map<String, String>> resultList = new ArrayList<>();
        List<String> items = splitByRule(sourceData, rule.getList());

        for (String itemData : items) {
            Map<String, String> data = new HashMap<>();
            // 章节名
            data.put("name", executeParse(itemData, rule.getName()));
            // 章节地址
            data.put("url", executeParse(itemData, rule.getUrl()));

            resultList.add(data);
        }
        return resultList;

    }

    /**
     * 解析正文图片 (Content)
     * 漫画的核心：一页里通常有几十张图，返回一个图片 URL 的 List
     */
    public List<String> parseContent(String sourceData,
                                     ComicSource.RuleContent rule) {
        String imageRule = rule.getImage();
        if (imageRule == null || imageRule.isEmpty()) return new ArrayList<>();

        for (RuleParser parser : parsers) {
            if (parser.match(imageRule)) {
                return parser.parseList(sourceData, imageRule);
            }
        }
        return new ArrayList<>();
    }
}

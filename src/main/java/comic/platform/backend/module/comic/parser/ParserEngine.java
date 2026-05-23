package comic.platform.backend.module.comic.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import comic.platform.backend.entity.ComicSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ParserEngine {

    // 自动收集所有实现了 RuleParser 接口的 Bean
    private final List<RuleParser> parsers;

    @PostConstruct
    public void init() {
        // 在这里进行优先级排序
        parsers.sort(Comparator.comparingInt(RuleParser::getPriority));
    }

    // Spring 自带的 JSON 神器
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行解析，通过 ## 分隔符串联多个处理步骤，清洁数据
     */
    public String executeParse(String sourceData,
                               String rule) {
        if (rule == null || rule.isEmpty()) return "";

        // 1. 用 "##" 切分规则管道
        String[] steps = rule.split("##");

        // 2. 初始化流转数据为原始网页/JSON数据
        String currentData = sourceData;

        // 3. 依次让数据穿过每一个解析节点
        for (String stepRule : steps) {
            // 防御性判断：跳过类似 "rule1####rule2" 中间产生的空段
            if (stepRule == null || stepRule.isEmpty()) {
                continue;
            }

            boolean matched = false;
            // 遍历所有解析器，寻找能处理当前 stepRule 的实现
            for (RuleParser parser : parsers) {
                if (parser.match(stepRule)) {
                    // 上一个解析器的输出 currentData，作为当前解析器的输入
                    currentData = parser.parse(currentData, stepRule);
                    matched = true;
                    break; // 找到合适的解析器后，进入管道的下一段
                }
            }

            // 如果当前这一段规则没有任何解析器认识，报错提示具体是哪一段出了问题
            if (!matched) {
                return "无法识别的规则语法: " + stepRule;
            }

            // 如果中间某一步提取的数据已经为空了，直接阻断管道，提前结束
            if (currentData == null || currentData.isEmpty()) {
                return "";
            }
        }

        // 4. 跑完全部管道后，返回最终加工出的字符串
        return currentData;
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
            // 兼容 $. 或 $[ 等常见的 JsonPath 前缀
            if (listRule.startsWith("$.") || listRule.startsWith("$[")) {
                // 1. JSON 切割模式：用 JsonPath 读出一个集合
                List<Object> list = JsonPath.read(sourceData, listRule);
                for (Object obj : list) {
                    // 处理 null 的情况，直接跳过以防后续抛出空指针异常
                    if (obj == null) {
                        continue;
                    }

                    // 如果本身就是字符串，说明是从类似 ["a", "b"] 的数组中提取的，直接强制转换
                    if (obj instanceof String) {
                        result.add((String) obj);
                    } else {
                        // 只有集合里的复杂对象（如 Map/子List），才需要重新序列化成 JSON 字符串
                        result.add(objectMapper.writeValueAsString(obj));
                    }
                }
            } else {
                // 2. HTML 切割模式：用 Jsoup 切割 DOM 树
                Elements elements = Jsoup.parse(sourceData).select(listRule);
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

package comic.platform.backend.parser;

import comic.platform.backend.entity.ComicSource;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * 智能路由解析方法
     */
    public String executeParse(String sourceData, String rule) {
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
     * 业务封装：解析搜索结果列表
     */
    public List<Map<String, String>> parseSearchList(String html, ComicSource.RuleSearch rule) {
        List<Map<String, String>> resultList = new ArrayList<>();

        // 容错：如果规则为空，直接返回空列表
        if (rule == null || rule.getList() == null || rule.getList().isEmpty()) {
            return resultList;
        }

        // 1. 先用 Jsoup 根据 rule.getList() 把HTML里面的列表切成单独的书
        //{"list": "tr.item", "name": "div.pl2 a@title", "cover": "a.nbg img@src", "detailUrl": "div.pl2 a@href", "searchUrl": "/top250"}
        Document doc = Jsoup.parse(html);
        Elements items = doc.select(rule.getList());

        // 2. 遍历每一个单独的书
        for (Element item : items) {
            Map<String, String> data = new HashMap<>();

            // 将当前的 Element 节点重新转回 String HTML 源码
            // 然后丢给你的万能执行器 executeParse 去智能提取！
            String itemHtml = item.outerHtml();

            data.put("name", executeParse(itemHtml, rule.getName()));
            data.put("cover", executeParse(itemHtml, rule.getCover()));
            data.put("detailUrl", executeParse(itemHtml, rule.getDetailUrl()));

            resultList.add(data);
        }

        return resultList;
    }
}

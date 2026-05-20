package comic.platform.backend.module.comic.parser;

import org.jsoup.nodes.Element;

import java.util.List;

public interface RuleParser {
    /**
     * 识别前缀
     * 根据规则字符串的前缀，判断当前解析器是否支持解析该规则
     */
    boolean match(String rule);

    /**
     * 执行解析
     * @param sourceData 抓取回来的网页源码 / 接口 JSON 文本
     * @param rule 数据库里存的规则字符串
     * @return 提取出的具体内容
     */
    String parse(String sourceData, String rule);

    /**
     * 提取漫画图
    **/
    List<String> parseList(String sourceData, String rule);
}

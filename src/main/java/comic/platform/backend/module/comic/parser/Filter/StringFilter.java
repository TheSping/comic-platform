package comic.platform.backend.module.comic.parser.Filter;

public interface StringFilter {
    /**
     * 过滤器的名字，用于匹配规则前缀，比如 "replace", "trim", "removeTags"
     */
    String name();

    /**
     * 执行过滤
     * @param input 上一个管道传过来的原始字符串
     * @param param 冒号后面的参数（如果有的话，比如 replace 的 regex,replacement）
     */
    String filter(String input, String param);
}

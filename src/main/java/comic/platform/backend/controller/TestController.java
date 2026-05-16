package comic.platform.backend.controller;

import comic.platform.backend.entity.RestBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user") // 不在白名单里，不登录就报错
public class TestController {

    @GetMapping("/me")
    public RestBean<String> me() {
        // 只有经过 JWT 过滤器完美验证的合法请求，才能执行到这里
        return RestBean.success("恭喜你，你已成功带着合法Token访问了内部保护区！");
    }
}
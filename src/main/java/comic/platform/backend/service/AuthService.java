package comic.platform.backend.service;

import comic.platform.backend.entity.RestBean;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestParam;

public interface AuthService {

    //发验证码
    public String getCode(@RequestParam String email,
                          @RequestParam String type);

    //注册
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String code,
                           @RequestParam String password);

    //登录
    public String login(@RequestParam String username,
                        @RequestParam String password);

    //找回密码
    public String resetPassword(@RequestParam String email,
                                @RequestParam String code,
                                @RequestParam String newPassword);

}

package comic.platform.backend.module.auth;

import comic.platform.backend.core.RestBean;
import comic.platform.backend.util.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @Resource
    private JwtUtils jwtUtils;

    //发验证码
    @PostMapping("/send-code")
    public RestBean<Void> sendCode(@RequestParam String email,
                                   @RequestParam String type) { // type 传 "register" 或 "reset"

        String result = authService.getCode(email, type);
        if (result == null) return RestBean.success(null);
        else return RestBean.failure(400, result);

    }

    //注册
    @PostMapping("/register")
    public RestBean<String> register(@RequestParam String username,
                                     @RequestParam String email,
                                     @RequestParam String code,
                                     @RequestParam String password) {
        // 调用 Service，直接获取结果描述
        String result = authService.register(username, email, code, password);

        // 如果返回 null，说明没出异常，注册成功
        if (result == null) {
            return RestBean.success(username);
        }

        // 否则，result 就是具体的失败原因（比如 "验证码错误"）
        return RestBean.failure(400, result);
    }

    //登录
    @PostMapping("/login")
    public RestBean<String> login(@RequestParam String username,
                                  @RequestParam String password) {
        // 返回 JWT Token 字符串
        String token = authService.login(username, password);

        if (token != null) {
            return RestBean.success(token);
        } else {
            return RestBean.failure(401, "用户名或密码错误");
        }
    }

    //登出
    @PostMapping("/logout")
    public RestBean<Void> logout(HttpServletRequest request) {
        // 从请求头中获取 Authorization 携带的 Token
        String authorization = request.getHeader("Authorization");

        // 将其丢进 Redis 黑名单
        if (jwtUtils.invalidate(authorization)) {
            return RestBean.success(null); // 登出成功
        } else {
            return RestBean.failure(400, "登出失败，Token 无效");
        }
    }

    //找回密码
    @PostMapping("/reset-password")
    public RestBean<String> resetPassword(@RequestParam String email,
                                          @RequestParam String code,
                                          @RequestParam String newPassword) {

        // 调用 Service，直接获取结果描述
        String result = authService.resetPassword(email, code, newPassword);

        // 如果返回 null，说明没出异常，返回新密码
        if (result == null) {
            return RestBean.success(null);
        }

        // 否则，result 就是具体的失败原因
        return RestBean.failure(400, result);

    }
}

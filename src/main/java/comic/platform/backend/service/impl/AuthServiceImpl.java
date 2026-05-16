package comic.platform.backend.service.impl;

import comic.platform.backend.entity.CUser;
import comic.platform.backend.mapper.UserMapper;
import comic.platform.backend.service.AuthService;
import comic.platform.backend.util.JwtUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private JavaMailSender sender;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserMapper mapper;
    //密码编码器
    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JwtUtils jwtUtils;

    @Value("${spring.mail.username}")
    private String fromEmail;

    //发验证码
    @Override
    public String getCode(@RequestParam String email, @RequestParam String type) {
        // 通过 type 检查邮箱是否已存在 或 需要找回密码
        if ("register".equals(type)) {
            if (mapper.selectByEmail(email) != null) return "该邮箱已被注册";
        } else if ("reset".equals(type)) {
            if (mapper.selectByEmail(email) == null) return "该邮箱尚未注册";
        } else {
            return "未知的验证码类型";
        }
        try {
            //验证码
            Random random = new Random();
            String code = String.valueOf(random.nextInt(900000) + 100000);

            //将验证码和邮箱存入 Redis，并设置 5 分钟过期
            String redisKey = "comic:code:" + type + ":" + email;
            stringRedisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);

            //发送邮件
            SimpleMailMessage message = new SimpleMailMessage();
            message.setSubject("在线漫画平台 - 注册验证码");
            message.setText("您的验证码是：" + code + " ，5分钟内有效。如非本人操作请忽略。");
            message.setFrom(fromEmail); // 使用读取到的 yml 配置
            message.setTo(email);
            sender.send(message);
            return null;
        } catch (Exception e) {
            return "发送失败";
        }
    }

    //注册
    @Override
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String code,
                           @RequestParam String password) {

        //从 Redis 中读取验证码
        String redisKey = "comic:code:register:" + email;
        String savedCode = stringRedisTemplate.opsForValue().get(redisKey);

        //校验
        if (savedCode == null) return "请先获取验证码";
        if (!savedCode.equals(code)) return "验证码错误";

        //用户名查重
        if (mapper.selectByUsername(username) != null) {
            return "该用户名已被注册，请换一个";
        }
        // 插入加密密码
        String encodedPassword = passwordEncoder.encode(password);
        CUser user = new CUser().setUsername(username).setPassword(encodedPassword).setEmail(email);
        mapper.insertUser(user);

        // 验证通过并完成操作后，删除验证码
        stringRedisTemplate.delete(redisKey);

        return null; // 代表成功
    }

    //登录
    @Resource
    private AuthenticationManager authenticationManager;

    @Override
    public String login(@RequestParam String username,
                        @RequestParam String password) {
        try {
            // 账号密码封装
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(username, password);

            // 调用 UserDetailsService 核对
            Authentication authenticate = authenticationManager.authenticate(token);

            // 获取认证通过的用户信息
            User user = (User) authenticate.getPrincipal();

            // 返回 jwt
            return jwtUtils.createJwt(user);

        } catch (AuthenticationException e) {
            return null;
        }
    }

    //找回密码
    @Override
    public String resetPassword(String email, String code, String newPassword) {
        // 从 Redis 中读取验证码
        String redisKey = "comic:code:reset:" + email;
        String savedCode = stringRedisTemplate.opsForValue().get(redisKey);

        //校验
        if (savedCode == null) return "请先获取验证码或验证码已过期";
        if (!savedCode.equals(code)) return "验证码错误";

        // 验证通过，将密码更新
        String encodedPassword = passwordEncoder.encode(newPassword);
        mapper.updatePasswordByEmail(email, encodedPassword);

        // 操作成功，销毁验证码
        stringRedisTemplate.delete(redisKey);

        return null;
    }
}

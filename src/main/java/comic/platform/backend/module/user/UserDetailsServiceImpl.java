package comic.platform.backend.module.user;

import comic.platform.backend.entity.CUser;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Resource
    private UserMapper mapper;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名去 MySQL 查询用户信息
        CUser user = mapper.selectByUsername(username);

        // 如果没查到这个人，抛出特定异常，大管家捕获后会认为登录失败
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        // 如果查到了，把你的 User 实体类包装成 Spring Security 认识的 UserDetails 对象返回
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword()) // 这里必须是数据库里查出来的、加密过的密码
                .roles("USER") // 暂时先给个默认角色，后面做权限控制时再细化
                .build();
    }
}

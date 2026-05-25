package comic.platform.backend.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;

@AllArgsConstructor
public class LoginUser implements UserDetails {

    // 提供暴露 ID 的方法，给 Controller 用
    @Getter
    private final Integer id;           // 我们最需要的 ID
    private final String username;      // 用户名
    private final String password;      // 密码（登录校验时需要）
    private final Collection<? extends GrantedAuthority> authorities; // 权限

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override
    public String getPassword() { return password; }
    @Override
    public String getUsername() { return username; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}
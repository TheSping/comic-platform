package comic.platform.backend.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import comic.platform.backend.util.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
//继承OncePerRequestFilter表示每次请求过滤一次，用于快速编写JWT校验规则

    @Resource
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request
            , HttpServletResponse response
            , FilterChain filterChain) throws ServletException, IOException {
        //首先从Header中取出JWT
        String authorization = request.getHeader("Authorization");
        //判断是否包含JWT且格式正确
        if (authorization != null && authorization.startsWith("Bearer ")) {
            UserDetails user = jwtUtils.resolveJwt(authorization);
            //如果 user 不为 null，说明 Token 合法、没过期、且不在黑名单中
            if(user != null) {
                //使用UsernamePasswordAuthenticationToken作为实体，填写相关用户信息
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                //塞给 SecurityContext 打上已登录标签
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        //放行，继续下一个过滤器
        //如果没有验证失败上面是不会给SecurityContext设置Authentication的，后面直接就被拦截掉了
        //有可能用户发起的是用户名密码登录请求，这种情况也要放行的
        filterChain.doFilter(request, response);
    }
}

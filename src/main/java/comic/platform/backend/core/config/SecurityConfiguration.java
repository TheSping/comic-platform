package comic.platform.backend.core.config;

import comic.platform.backend.core.RestBean;
import comic.platform.backend.core.filter.JwtAuthenticationFilter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;


@Configuration
public class SecurityConfiguration {

    //告知security用我们自己的登录逻辑
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    //验证时对密码的编码规则
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //jwt的过滤器
    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http
                //放开/api/auth/**，别的拦截验证JWT
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/send-code",
                                "/api/auth/reset-password",
                                "/api/comic/**",
                                "/api/proxy/**",
                                "/api/source/**",
                                "/api/bookshelf/**",
                                "/doc.html",                // Knife4j UI 页面
                                "/webjars/**",              // Knife4j 静态资源
                                "/v3/api-docs/**",          // OpenAPI 3.0 数据接口 (极其重要，用来生成左侧菜单的)
                                "/swagger-resources/**",    // Swagger 资源
                                "/error")
                        .permitAll()
                        .anyRequest().authenticated()
                )
                //处理跨域
                .cors(conf -> {
                    CorsConfiguration cors = new CorsConfiguration(); //打开 Spring Security 的跨域配置开关。
                    cors.addAllowedOrigin("http://localhost:8080");  //添加前端站点地址，这样就可以告诉浏览器信任了
                    cors.setAllowCredentials(true); //允许跨域请求中携带Cookie
                    cors.addAllowedHeader("*");
                    cors.addAllowedMethod("*");
                    cors.addExposedHeader("*");
                    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                    source.registerCorsConfiguration("/**", cors); //把前面写的cors规则应用到所有地址
                    conf.configurationSource(source);
                })
                //异常处理
                .exceptionHandling(conf -> {
                    //权限不足
                    conf.accessDeniedHandler(this::onAccessDeny);
                    //未登录
                    conf.authenticationEntryPoint(this::onAuthenticationFailure);
                })
                //将Session管理创建策略改成无状态，使用JWT方案
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(conf -> {
                    conf.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                //添加我们用于处理JWT的过滤器到Security过滤器链中
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }


    private void onAccessDeny(HttpServletRequest request,
                              HttpServletResponse response,
                              AccessDeniedException exception) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        String json = RestBean.failure(403, "抱歉，您没有权限访问此接口").asJsonString();
        response.getWriter().write(json);
    }

    private void onAuthenticationFailure(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        String json = RestBean.failure(401, "请先登录或Token已失效").asJsonString();
        response.getWriter().write(json);
    }

}


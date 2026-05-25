package comic.platform.backend.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import comic.platform.backend.core.LoginUser;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtils {

    @Resource
    private StringRedisTemplate stringRedisTemplate; // 注入 Redis

    //Jwt秘钥
    private static final String key = "nanwuamituofotoukeydeshigou";

    //根据用户信息创建Jwt令牌
    public String createJwt(UserDetails user) {
        LoginUser loginUser = (LoginUser) user;

        Algorithm algorithm = Algorithm.HMAC256(key);
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        calendar.add(Calendar.SECOND, 3600 * 24 * 7);// 7天过期

        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withClaim("id", loginUser.getId())
                .withClaim("name", user.getUsername())  // 配置JWT自定义信息
                .withClaim("authorities", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .withExpiresAt(calendar.getTime())  // 设置过期时间
                .withIssuedAt(now)    // 设置创建创建时间
                .sign(algorithm);   // 最终签名
    }

    //根据Jwt验证并解析用户信息
    public UserDetails resolveJwt(String token) {
        // 兼容前端可能传来的 "Bearer " 前缀
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            //对JWT令牌进行验证，看看是否被修改以及是否过期
            DecodedJWT verify = jwtVerifier.verify(token);
            // 判断UUID是否在 Redis 黑名单中
            if (isInvalid(verify.getId())) return null;

            //从Token中掏数据
            Map<String, Claim> claims = verify.getClaims();
            Integer userId = claims.get("id").asInt();
            String username = claims.get("name").asString();
            String[] authoritiesArray = claims.get("authorities").asArray(String.class);
            List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(authoritiesArray);

            // 把这三个核心数据重新拼装成 LoginUser，密码置空（解析 Token 时不需要密码）
            return new LoginUser(userId, username, "", authorities);

        } catch (JWTVerificationException e) {
            return null;
        }
    }

    //将 Token 加入 Redis 黑名单
    public boolean invalidate(String token) {
        // 去除可能存在的 "Bearer " 前缀
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            DecodedJWT verify = jwtVerifier.verify(token); //对JWT令牌进行验证，看看是否被修改
            String jwtId = verify.getId(); // 拿到 JWT 的唯一 UUID

            if (isInvalid(jwtId)) {
                return false;
            }
            // 计算该 Token 距离过期还剩多少时间
            Date expiresAt = verify.getExpiresAt();
            long remainTime = expiresAt.getTime() - new Date().getTime();

            if (remainTime > 0) {
                // 将 UUID 作为 Key 存入 Redis，Value 随便设个 "1"
                // 过期时间设置为 Token 的剩余寿命（毫秒转成毫秒单位存入）
                stringRedisTemplate.opsForValue().set(
                        "comic:jwt:blacklist:" + jwtId,
                        "1",
                        remainTime,
                        TimeUnit.MILLISECONDS
                );
            }
            return true;
        } catch (JWTVerificationException e) {
            return false; // Token 本身就是伪造或早就过期的，无需拉黑
        }
    }

    public boolean isInvalid(String jwtId) {
        return stringRedisTemplate.hasKey("comic:jwt:blacklist:" + jwtId);
    }
}

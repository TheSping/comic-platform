package comic.platform.backend.util;

import comic.platform.backend.core.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全服务工具类
 */
public class SecurityUtils {

    // ================== 1. 底层基座 (最核心的判空逻辑只写一次) ==================

    /**
     * 获取当前用户信息
     * @return LoginUser，如果未登录则返回 null
     */
    public static LoginUser getLoginUserSafe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    // ================== 2. 严格模式 (专供必须登录的接口使用) ==================

    /**
     * 判断登录信息是否正常获取
     */
    public static LoginUser getLoginUser() {
        LoginUser user = getLoginUserSafe();
        if (user == null) {
            throw new RuntimeException("获取当前用户信息失败，用户未登录或登录已过期");
        }
        return user;
    }

    /**
     * 获取当前登录用户的 ID
     */
    public static Integer getUserId() {
        return getLoginUser().getId();
    }

    /**
     * 获取当前登录用户的用户名
     */
    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    // ================== 3. 兼容模式 (专供支持游客的接口使用) ==================

    /**
     * 获取当前用户的 ID（支持游客模式）
     * @return 正常用户返回真实 ID，游客返回 -1
     */
    public static Integer getUserIdSafe() {
        LoginUser user = getLoginUserSafe();
        return user != null ? user.getId() : -1;
    }
}
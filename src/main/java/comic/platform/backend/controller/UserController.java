package comic.platform.backend.controller;

import comic.platform.backend.entity.RestBean;
import comic.platform.backend.entity.dto.UpdateProfileDTO;
import comic.platform.backend.entity.vo.UserVo;
import comic.platform.backend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    UserService userService;

    @GetMapping("/info")
    public RestBean<UserVo> getUserInfo(Principal principal) {
        String username = principal.getName();
        return RestBean.success(userService.getUserInfoByUsername(username));
    }

    @PostMapping("/update")
    public RestBean<Void> updateUserInfo(Principal principal,
                                         @RequestBody UpdateProfileDTO dto) {
        String username = principal.getName();
        String result = userService.updateUserInfo(username, dto);
        if (result == null) {
            return RestBean.success(null);
        }
        return RestBean.failure(400, result);
    }
}

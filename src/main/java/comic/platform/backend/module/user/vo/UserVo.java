package comic.platform.backend.module.user.vo;

import lombok.Data;

@Data
public class UserVo {
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String gender;
    private String role;
}

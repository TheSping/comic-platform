package comic.platform.backend.module.user;

import comic.platform.backend.module.user.dto.UpdateProfileDTO;
import comic.platform.backend.module.user.vo.UserVo;

public interface UserService {
    UserVo getUserInfoByUsername(String username);

    String updateUserInfo(String username, UpdateProfileDTO dto);
}

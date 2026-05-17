package comic.platform.backend.service;

import comic.platform.backend.entity.dto.UpdateProfileDTO;
import comic.platform.backend.entity.vo.UserVo;

public interface UserService {
    UserVo getUserInfoByUsername(String username);

    String updateUserInfo(String username, UpdateProfileDTO dto);
}

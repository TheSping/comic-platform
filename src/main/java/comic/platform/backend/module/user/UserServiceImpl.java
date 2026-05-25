package comic.platform.backend.module.user;

import comic.platform.backend.module.user.dto.UpdateProfileDTO;
import comic.platform.backend.module.user.vo.UserVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    UserMapper mapper;

    @Override
    public UserVo getUserInfoByUsername(String username) {
        return mapper.getUserInfoByUsername(username);
    }

    @Override
    public String updateUserInfo(String username, UpdateProfileDTO dto) {
        // 先通过用户名找到 c_user 表里的 ID
        CUser user = mapper.selectByUsername(username);
        if (user == null) {
            return "用户不存在";
        }
        Integer userId = user.getId(); // 拿到主表的 id

        // 去 c_user_profile 看看这个 userId 以前有没有过资料
        CUserProfile profile = mapper.selectProfileByUserId(userId);

        // 准备好要写入/更新的数据
        CUserProfile newProfile = new CUserProfile()
                .setUserId(userId)
                .setNickname(dto.getNickname())
                .setAvatar(dto.getAvatar())
                .setGender(dto.getGender());

        if (profile == null) {
            // 第一次来修改资料，执行 INSERT
            newProfile.setRole("ROLE_USER");//给一个默认权限
            mapper.insertProfile(newProfile);
        } else {
            // 表里有记录，执行 UPDATE
            mapper.updateProfile(newProfile);
        }

        //通过返回null
        return null;
    }

}

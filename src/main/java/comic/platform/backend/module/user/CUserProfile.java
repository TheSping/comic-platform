package comic.platform.backend.module.user;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("c_user_profile")
public class CUserProfile {
    private Integer id;
    private Integer userId;
    private String nickname;
    private String avatar;
    private String gender;
    private String role;
}

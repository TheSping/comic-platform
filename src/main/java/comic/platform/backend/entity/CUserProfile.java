package comic.platform.backend.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CUserProfile {
    private Integer id;
    private Integer userId;
    private String nickname;
    private String avatar;
    private String gender;
    private String role;
}

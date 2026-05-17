package comic.platform.backend.entity.dto;

import lombok.Data;

@Data
public class UpdateProfileDTO {
    private String nickname;
    private String avatar;
    private String gender;
}

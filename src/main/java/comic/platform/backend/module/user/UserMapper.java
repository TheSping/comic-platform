package comic.platform.backend.module.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import comic.platform.backend.entity.CUser;
import comic.platform.backend.entity.CUserProfile;
import comic.platform.backend.entity.vo.UserVo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.data.repository.query.Param;

@Mapper
public interface UserMapper extends BaseMapper<CUser> {
    // 根据名字查询用户
    @Select("SELECT * FROM c_user WHERE username = #{username}")
    CUser selectByUsername(String username);

    // 根据邮箱查询用户
    @Select("SELECT * FROM c_user WHERE email = #{email}")
    CUser selectByEmail(String email);

    // 根据邮箱更新密码
    @Update("UPDATE c_user SET password = #{password} WHERE email = #{email}")
    int updatePasswordByEmail(@Param("email") String email, @Param("password") String password);

    //注册
    @Insert("INSERT INTO c_user (username, password,email) VALUES (#{username}, #{password},#{email})")
    int insertUser(CUser user);

    //查询用户信息
    @Select("SELECT u.username,u.email,p.nickname,p.avatar,p.gender,p.role " +
            "FROM c_user AS u " +
            "LEFT JOIN c_user_profile AS p ON u.id = p.user_id WHERE u.username = #{username}")
    UserVo getUserInfoByUsername(String username);

    // 根据 user_id 探测详情表里有没有记录
    @Select("SELECT * FROM c_user_profile WHERE user_id = #{userId}")
    CUserProfile selectProfileByUserId(Integer userId);

    // 针对新用户的第一次修改，执行插入 (INSERT)
    @Insert("INSERT INTO c_user_profile (user_id, nickname, avatar, gender, role) " +
            "VALUES (#{userId}, #{nickname}, #{avatar}, #{gender}, #{role})")
    int insertProfile(CUserProfile profile);

    // 针对老用户的后续修改，执行更新 (UPDATE)
    @Update("UPDATE c_user_profile SET nickname = #{nickname}, avatar = #{avatar}, " +
            "gender = #{gender} WHERE user_id = #{userId}")
    int updateProfile(CUserProfile profile);
}

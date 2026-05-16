package comic.platform.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import comic.platform.backend.entity.CUser;
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

    @Insert("INSERT INTO c_user (username, password,email) VALUES (#{username}, #{password},#{email})")
    int insertUser(CUser user);
}

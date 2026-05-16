package comic.platform.backend;

import comic.platform.backend.mapper.UserMapper;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class ComicPlatformBackendApplicationTests {
    @Resource
    UserMapper mapper;

    @Test
    void contextLoads() {

    }

}

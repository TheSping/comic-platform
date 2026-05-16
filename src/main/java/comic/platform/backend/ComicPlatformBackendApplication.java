package comic.platform.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ComicPlatformBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComicPlatformBackendApplication.class, args);
    }

}

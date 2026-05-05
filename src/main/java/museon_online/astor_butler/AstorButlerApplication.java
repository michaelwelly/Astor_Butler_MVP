package museon_online.astor_butler;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AstorButlerApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            if (System.getProperty(key) == null && System.getenv(key) == null) {
                System.setProperty(key, entry.getValue());
            }
        });
        SpringApplication.run(AstorButlerApplication.class, args);
    }

}

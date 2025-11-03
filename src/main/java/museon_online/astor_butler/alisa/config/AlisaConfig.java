package museon_online.astor_butler.alisa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AlisaConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableScheduling
@EnableJpaRepositories(basePackages = "repository")
@EntityScan(basePackages = "entity")
@SpringBootApplication(excludeName = {
        "org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration",
        "org.springframework.boot.autoconfigure.r2dbc.R2dbcDataAutoConfiguration",
        "org.springframework.boot.autoconfigure.r2dbc.R2dbcRepositoriesAutoConfiguration",
        "org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration"
})
@ComponentScan(basePackages = {"controller", "service", "config", "security", "consumer", "cache", "model", "webSocket", "entity", "repository"})
public class gatewayApiApp {
    
    public static void main(String[] args) {
        SpringApplication.run(gatewayApiApp.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*").allowedMethods("*");
            }
        };
    }
}

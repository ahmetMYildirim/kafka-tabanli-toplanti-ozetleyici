import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"config", "producer", "poller", "consumer"})
public class MeetingStreamingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeetingStreamingServiceApplication.class, args);
    }
}

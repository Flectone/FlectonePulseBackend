package net.flectone.pulse.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlectonePulseBackendApplication {

//    @Bean
//    CommandLineRunner initDatabase(MetricsService generatorService) {
//        return args -> {
//            generatorService.generateAndSaveTestData();
//        };
//    }

    public static void main(String[] args) {
        SpringApplication.run(FlectonePulseBackendApplication.class, args);
    }

}

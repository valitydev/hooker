package dev.vality.hooker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ServletComponentScan
@SpringBootApplication(scanBasePackages = {"dev.vality.hooker"})
@EnableScheduling
public class HookerApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(HookerApplication.class, args);
    }

}

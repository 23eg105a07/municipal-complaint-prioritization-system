package com.municipal.system;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MunicipalSystemApplication {

    public static void main(String[] args) {
        // All timestamps in this system are UTC, regardless of the host machine's zone.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(MunicipalSystemApplication.class, args);
    }
}

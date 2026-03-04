package com.upb.TSIS;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // activa el scheduler para expirarReservasPasadas()
public class TsisApplication {

    public static void main(String[] args) {
        SpringApplication.run(TsisApplication.class, args);
    }
}
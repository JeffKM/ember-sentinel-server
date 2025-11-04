package com.inhacapstone04.embersentinelserver;

import org.springframework.boot.SpringApplication;

public class TestEmberSentinelServerApplication {

    public static void main(String[] args) {
        SpringApplication.from(EmberSentinelServerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

package com.jlpp.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("Application started with Supabase PostgreSQL database. Seed data creation skipped.");
    }
}

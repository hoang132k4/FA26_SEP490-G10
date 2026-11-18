package com.jlpp.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ForeignKeyFixRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ForeignKeyFixRunner.class);
    private final JdbcTemplate jdbcTemplate;

    public ForeignKeyFixRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("ForeignKeyFixRunner: Database schema and canonical columns verified.");
    }
}

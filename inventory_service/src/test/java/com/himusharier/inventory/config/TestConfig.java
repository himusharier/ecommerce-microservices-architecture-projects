package com.himusharier.inventory.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;

@TestConfiguration
@Profile("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class TestConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                .username("sa")
                .password("")
                .build();
    }
}

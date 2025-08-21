package com.example.migrate;

import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import com.mongodb.ConnectionString;

@Configuration
public class MongoMultiConfig {

    @Configuration
    @EnableMongoRepositories(
        basePackages = "com.example.migrate.repository.source",
        mongoTemplateRef = "sourceMongoTemplate"
    )
    public static class SourceConfiguration {
        @Value("${data.mongodb.source.uri}")
        private String sourceMongoUri;

        @Primary // Đánh dấu là primary để Spring sử dụng mặc định cho các component phụ thuộc
        @Bean(name = "sourceMongoTemplate")
        public MongoTemplate sourceMongoTemplate() {
            // Sử dụng constructor chỉ nhận một URI string
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(sourceMongoUri));
        }
    }

    @Configuration
    @EnableMongoRepositories(
        basePackages = "com.example.migrate.repository.target",
        mongoTemplateRef = "targetMongoTemplate"
    )
    public static class TargetConfiguration {
        @Value("${data.mongodb.target.uri}")
        private String targetMongoUri;

        @Bean(name = "targetMongoTemplate")
        public MongoTemplate targetMongoTemplate() {
            // Sử dụng constructor chỉ nhận một URI string
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(targetMongoUri));
        }
    }
}

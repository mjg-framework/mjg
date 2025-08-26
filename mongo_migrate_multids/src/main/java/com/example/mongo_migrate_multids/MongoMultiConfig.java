package com.example.mongo_migrate_multids;

import com.example.migrate.repository.self.common.SelfMongoRepositoryInterface;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import com.mongodb.ConnectionString;

@Configuration
public class MongoMultiConfig {

    @Configuration
    @EnableMongoRepositories(
        basePackages = "com.example.migrate.repository.source",
        mongoTemplateRef = "sourceMongoTemplate",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = ".*\\.common\\..*"
        )
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
        mongoTemplateRef = "targetMongoTemplate",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = ".*\\.common\\..*"
        )
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

    @Configuration
    @EnableMongoRepositories(
        basePackages = "com.example.migrate.repository.self",
        mongoTemplateRef = "selfMongoTemplate",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = ".*\\.common\\..*"
        )
    )
    public static class SelfConfiguration {
        @Value("${data.mongodb.self.uri}")
        private String selfMongoUri;

        @Bean(name = "selfMongoTemplate")
        public MongoTemplate selfMongoTemplate() {
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(selfMongoUri));
        }
    }

}

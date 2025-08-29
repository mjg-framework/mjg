package com.example.mongo_migrate_multids;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
public class MongoMultiConfig {

    @Configuration
    @EnableMongoRepositories(
        basePackages = "com.example.mongo_migrate_multids.repository.src",
        mongoTemplateRef = "sourceMongoTemplate",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = ".*\\.common\\..*"
        )
    )
    public static class SrcConfiguration {
        @Value("${data.mongodb.source.uri}")
        private String srcMongoUri;

        @Primary // Đánh dấu là primary để Spring sử dụng mặc định cho các component phụ thuộc
        @Bean(name = "sourceMongoTemplate")
        public MongoTemplate sourceMongoTemplate() {
            // Sử dụng constructor chỉ nhận một URI string
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(srcMongoUri));
        }
    }

    @Configuration
    @EnableMongoRepositories(
        basePackages = "com.example.mongo_migrate_multids.repository.dest",
        mongoTemplateRef = "destMongoTemplate",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = ".*\\.common\\..*"
        )
    )
    public static class DestConfiguration {
        @Value("${data.mongodb.dest.uri}")
        private String destMongoUri;

        @Bean(name = "destMongoTemplate")
        public MongoTemplate destMongoTemplate() {
            // Sử dụng constructor chỉ nhận một URI string
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(destMongoUri));
        }
    }
}

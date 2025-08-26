package com.example.mongo_migrate_multids;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@SpringBootApplication
public class MongoMigrateApplication {
    public static void main(String[] args) {
        // Thêm thuộc tính để vô hiệu hóa tự động cấu hình GridFS
        System.setProperty("spring.data.mongodb.gridfs.enabled", "false");
        SpringApplication.run(MongoMigrateApplication.class, args);
    }
}

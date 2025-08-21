package com.example.migrate.runner;

import com.example.migrate.service.MigrationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MigrateRunner implements CommandLineRunner {

    private final MigrationService migrationService;

    public MigrateRunner(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void run(String... args) {
        // Sử dụng repository để migrate users
        migrationService.migrateUsers(1000);
        
        // Hoặc có thể sử dụng phương thức cũ nếu cần
        // migrationService.migrateCollection(UserEntity.class, "users", 1000);
    }
}

package com.example.mongo_migrate_multids.runners;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.mongo_migrate_multids.services.MigrateToTWService;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class MigrationsRunner implements CommandLineRunner {
    private final MigrateToTWService migrateToTWService;

    @Override
    public void run(String... args) {
        migrateToTWService.run();
    }
}

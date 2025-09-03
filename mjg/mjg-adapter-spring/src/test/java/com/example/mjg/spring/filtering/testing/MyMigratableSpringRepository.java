package com.example.mjg.spring.filtering.testing;

import com.example.mjg.spring.repositories.MigratableSpringRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MyMigratableSpringRepository
extends MigratableSpringRepository<MyEntity, Integer>
{
}

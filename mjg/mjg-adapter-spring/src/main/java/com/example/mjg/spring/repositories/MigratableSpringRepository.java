package com.example.mjg.spring.repositories;

import com.example.mjg.data.MigratableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.Set;

public interface MigratableSpringRepository<T extends MigratableEntity, ID extends Serializable> {
    Page<T> findAll(Pageable pageable);

    Page<T> findAllByIdInOrderByIdAsc(Set<ID> ids, Pageable pageable);

    <S extends T> S save(S entity);

    <S extends T> Iterable<S> saveAll(Iterable<S> entities);
}

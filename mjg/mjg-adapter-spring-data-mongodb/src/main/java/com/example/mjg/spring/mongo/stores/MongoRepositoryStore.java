package com.example.mjg.spring.mongo.stores;

import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.DuplicateDataException;
import com.example.mjg.spring.stores.SpringRepositoryStore;
import com.mongodb.DuplicateKeyException;

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public abstract class MongoRepositoryStore<
    T extends MigratableEntity,
    ID extends Serializable
>
extends SpringRepositoryStore<T, ID>
{
    public abstract MongoTemplate getMongoTemplate();

    /**
     * In case of no transaction manager
     * e.g. MongoDB is not run as replica set,
     * just return null.
     */
    public abstract MongoTransactionManager getTxManager();





    private volatile TransactionTemplate txTemplate = null;

    /**
     * MongoDB transactions require special logic
     */
    @Override
    protected void doSaveAll(List<T> records) throws Exception {
        if (records.isEmpty()) return;
        
        MongoTransactionManager txManager = getTxManager();

        try {
            if (txManager != null) {
                if (txTemplate == null) {
                    txTemplate = new TransactionTemplate(txManager);
                }
                txTemplate.execute(status -> {
                    getRepository().saveAll(records);
                    return null;
                });
            } else {
                // Transaction not supported, fall back to risky saveAll().
                // Users are now responsible for data correctness. To be
                // absolutely sure, set batch size on save = 1 i.e.
                // @TransformAndSaveTo(..., batchSize = 1)

                if (records.size() > 1) {
                    log.debug("Using saveAll() on MongoDB on several records do not guarantee transactional atomicity. Consider running MongoDB as replica set, or for safety, set @TransformAndSaveTo(..., batchSize = 1) (which is way slower, but viable if you can't manipulate the database.)");
                }

                getRepository().saveAll(records);
            }
        } catch (DataIntegrityViolationException | DuplicateKeyException e) {
            throw new DuplicateDataException(e);
        }
    }

    @Override
    protected void doSave(T record) throws Exception {
        try {
            getRepository().save(record);
        } catch (DataIntegrityViolationException | DuplicateKeyException e) {
            throw new DuplicateDataException(e);
        }
    }
}

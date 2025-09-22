package com.example.mongo_migrate_multids;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.index.IndexDefinition;

import java.util.List;
import java.util.stream.StreamSupport;

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

        @Bean(name = "sourceMongoTemplate")
        public MongoTemplate sourceMongoTemplate() {
            MongoTemplate mongoTemplate = new MongoTemplate(new SimpleMongoClientDatabaseFactory(srcMongoUri));
            
            // Enable automatic index creation
            MappingMongoConverter converter = (MappingMongoConverter) mongoTemplate.getConverter();
            MongoMappingContext mappingContext = (MongoMappingContext) converter.getMappingContext();
            mappingContext.setAutoIndexCreation(true);
            
            return mongoTemplate;
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

        @Primary // Đánh dấu là primary để Spring sử dụng mặc định cho các component phụ thuộc
        @Bean(name = "destMongoTemplate")
        public MongoTemplate destMongoTemplate() {
            MongoTemplate mongoTemplate = new MongoTemplate(new SimpleMongoClientDatabaseFactory(destMongoUri));
            
            // Enable automatic index creation
            MappingMongoConverter converter = (MappingMongoConverter) mongoTemplate.getConverter();
            MongoMappingContext mappingContext = (MongoMappingContext) converter.getMappingContext();
            mappingContext.setAutoIndexCreation(true);
            
            return mongoTemplate;
        }
    }

    @Component
    public static class IndexInitializer {

        @Autowired
        @Qualifier("sourceMongoTemplate")
        private MongoTemplate sourceMongoTemplate;

        @Autowired
        @Qualifier("destMongoTemplate")
        private MongoTemplate destMongoTemplate;

        @EventListener(ContextRefreshedEvent.class)
        public void initializeIndexes() {
            // createIndexes(sourceMongoTemplate);
            createIndexes(destMongoTemplate);
        }

        private void createIndexes(MongoTemplate mongoTemplate) {
            MongoMappingContext mappingContext = (MongoMappingContext) mongoTemplate.getConverter().getMappingContext();
            
            // Get all persistent entities
            for (MongoPersistentEntity<?> persistentEntity : mappingContext.getPersistentEntities()) {
                Class<?> clazz = persistentEntity.getType();
                
                // Check if the class is annotated with @Document
                if (clazz.isAnnotationPresent(Document.class)) {
                    IndexOperations indexOps = mongoTemplate.indexOps(clazz);
                    
                    // Create resolver for indexes defined on the entity
                    MongoPersistentEntityIndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);
                    
                    // Resolve and ensure indexes
                    @SuppressWarnings("unchecked")
                    List<IndexDefinition> indexDefinitions = (List<IndexDefinition>)
                        StreamSupport.stream(
                            resolver.resolveIndexFor(clazz).spliterator(), false
                        ).toList();
                    
                    for (IndexDefinition indexDefinition : indexDefinitions) {
                        indexOps.ensureIndex(indexDefinition);
                    }
                    
                    System.out.println("Created indexes for: " + clazz.getSimpleName() + " in database: " + 
                                     mongoTemplate.getDb().getName());
                }
            }
        }
    }
}
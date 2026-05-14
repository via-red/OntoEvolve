package com.ontoevolve.saas.store;

import com.ontoevolve.saas.config.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DomainStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(DomainStoreFactory.class);

    public DomainStore createStore(StoreConfig storeConfig) {
        DomainStore store = switch (storeConfig.getType()) {
            case "memory" -> new MemoryDomainStore();
            case "neo4j" -> throw new UnsupportedOperationException(
                    "Neo4jDomainStore not yet implemented. Use type: memory");
            default -> throw new IllegalArgumentException(
                    "Unknown store type: " + storeConfig.getType());
        };

        Map<String, Object> config = storeConfig.getConfig();
        if (config != null) {
            store.initialize(config);
        }

        log.info("Created DomainStore: type={}", storeConfig.getType());
        return store;
    }
}

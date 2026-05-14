package com.ontoevolve.saas.store;

import com.ontoevolve.saas.config.DomainDefinition;
import com.ontoevolve.saas.config.DomainStatus;
import com.ontoevolve.saas.service.DomainConfigService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of all active domain stores.
 * Creates stores on publish, destroys on unpublish.
 */
@Component
public class DomainStoreManager {

    private static final Logger log = LoggerFactory.getLogger(DomainStoreManager.class);

    private final Map<String, DomainStore> stores = new ConcurrentHashMap<>();
    private final DomainConfigService configService;
    private final DomainStoreFactory storeFactory;

    public DomainStoreManager(DomainConfigService configService, DomainStoreFactory storeFactory) {
        this.configService = configService;
        this.storeFactory = storeFactory;
        initializePublishedStores();
    }

    private void initializePublishedStores() {
        for (DomainDefinition def : configService.getPublishedDomains()) {
            registerStore(def);
        }
    }

    public DomainStore getStore(String domainId) {
        DomainStore store = stores.get(domainId);
        if (store == null) {
            throw new IllegalStateException("No store found for domain: " + domainId
                    + ". Is the domain published?");
        }
        return store;
    }

    public void registerStore(DomainDefinition def) {
        if (def.getDomain().getStatus() != DomainStatus.PUBLISHED) {
            log.warn("Cannot register store for non-published domain: {}", def.getId());
            return;
        }
        DomainStore store = storeFactory.createStore(def.getDomain().getStore());
        stores.put(def.getId(), store);
        log.info("Registered store for domain: {} ({})", def.getDomain().getName(), def.getId());
    }

    public void unregisterStore(String domainId) {
        DomainStore store = stores.remove(domainId);
        if (store != null) {
            store.close();
            log.info("Unregistered store for domain: {}", domainId);
        }
    }

    public boolean hasStore(String domainId) {
        return stores.containsKey(domainId);
    }

    @PreDestroy
    public void closeAll() {
        stores.forEach((id, store) -> store.close());
        stores.clear();
    }
}

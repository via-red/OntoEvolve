package com.ontoevolve.saas.service;

import com.ontoevolve.saas.config.DomainDefinition;
import com.ontoevolve.saas.store.DomainStoreManager;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around DomainStoreManager for admin operations.
 * Provides clean interface for register/unregister used by AdminDomainController.
 */
@Service
public class DomainStoreManagerService {

    private final DomainStoreManager storeManager;

    public DomainStoreManagerService(DomainStoreManager storeManager) {
        this.storeManager = storeManager;
    }

    public void register(DomainDefinition def) {
        storeManager.registerStore(def);
    }

    public void unregister(String domainId) {
        storeManager.unregisterStore(domainId);
    }
}

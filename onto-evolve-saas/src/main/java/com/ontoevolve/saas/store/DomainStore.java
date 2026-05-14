package com.ontoevolve.saas.store;

import com.ontoevolve.core.model.Feedback;
import com.ontoevolve.core.model.InputEvent;
import com.ontoevolve.core.spi.PopulationStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Domain-specific data store — extends PopulationStore with record/feedback persistence.
 * Each domain gets its own instance, configured via domain.json → store config.
 */
public interface DomainStore extends PopulationStore {

    /** Save an event record */
    void saveRecord(InputEvent record);

    /** List records with optional filters (paginated) */
    List<InputEvent> getRecords(Map<String, Object> filters, int page, int size);

    /** Get a single record by ID */
    Optional<InputEvent> getRecord(String recordId);

    /** Count records matching filters */
    long countRecords(Map<String, Object> filters);

    /** Save a feedback evaluation */
    void saveFeedback(Feedback feedback);

    /** Get all feedbacks for a record */
    List<Feedback> getFeedbacks(String recordId);

    /** Initialize the store with config params from domain.json */
    void initialize(Map<String, Object> config);

    /** Close/cleanup the store connection */
    void close();
}

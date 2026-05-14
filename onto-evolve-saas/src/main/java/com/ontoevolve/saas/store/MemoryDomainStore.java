package com.ontoevolve.saas.store;

import com.ontoevolve.core.model.Feedback;
import com.ontoevolve.core.model.InputEvent;
import com.ontoevolve.core.store.InMemoryPopulationStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of DomainStore.
 * Extends InMemoryPopulationStore for population management,
 * adds record/feedback storage via ConcurrentHashMap.
 */
public class MemoryDomainStore extends InMemoryPopulationStore implements DomainStore {

    private final Map<String, InputEvent> records = new ConcurrentHashMap<>();
    private final Map<String, List<Feedback>> feedbacks = new ConcurrentHashMap<>();

    @Override
    public void saveRecord(InputEvent record) {
        records.put(record.getId(), record);
    }

    @Override
    public List<InputEvent> getRecords(Map<String, Object> filters, int page, int size) {
        return records.values().stream()
                .filter(r -> matches(r, filters))
                .sorted(Comparator.comparing(InputEvent::getTimestamp).reversed())
                .skip((long) (page - 1) * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InputEvent> getRecord(String recordId) {
        return Optional.ofNullable(records.get(recordId));
    }

    @Override
    public long countRecords(Map<String, Object> filters) {
        return records.values().stream()
                .filter(r -> matches(r, filters))
                .count();
    }

    @Override
    public void saveFeedback(Feedback feedback) {
        String executionId = feedback.getExecution() != null
                ? feedback.getExecution().getIri()
                : "unknown";
        feedbacks.computeIfAbsent(executionId, k -> Collections.synchronizedList(new ArrayList<>()));
        feedbacks.get(executionId).add(feedback);
    }

    @Override
    public List<Feedback> getFeedbacks(String recordId) {
        return feedbacks.getOrDefault(recordId, List.of());
    }

    @Override
    public void initialize(Map<String, Object> config) {
        // Memory store needs no initialization
    }

    @Override
    public void close() {
        // Memory store needs no cleanup
    }

    private boolean matches(InputEvent record, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) return true;
        for (Map.Entry<String, Object> f : filters.entrySet()) {
            String key = f.getKey();
            Object value = f.getValue();
            if (value == null || value.toString().isEmpty()) continue;
            if ("description".equals(key) && record.getRawDescription() != null) {
                if (!record.getRawDescription().contains(value.toString())) return false;
            } else if ("subjectId".equals(key) && record.getSubjectId() != null) {
                if (!record.getSubjectId().equals(value.toString())) return false;
            } else {
                Object attr = record.getAttributes().get(key);
                if (attr == null || !attr.toString().contains(value.toString())) return false;
            }
        }
        return true;
    }
}

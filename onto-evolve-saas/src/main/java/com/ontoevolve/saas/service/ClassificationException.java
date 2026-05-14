package com.ontoevolve.saas.service;

/**
 * Thrown when event classification fails — no matching rule,
 * LLM error without fallback, or manual approval required.
 */
public class ClassificationException extends RuntimeException {

    public ClassificationException(String message) {
        super(message);
    }

    public ClassificationException(String message, Throwable cause) {
        super(message, cause);
    }
}

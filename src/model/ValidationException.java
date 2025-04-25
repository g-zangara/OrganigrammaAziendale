package model;

/**
 * Exception thrown when validation rules are violated.
 */
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor with error message
     * @param message The error message
     */
    public ValidationException(String message) {
        super(message);
    }
}
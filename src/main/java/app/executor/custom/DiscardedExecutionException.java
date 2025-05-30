package app.executor.custom;

public class DiscardedExecutionException extends RuntimeException {
    public DiscardedExecutionException(String message) {
        super(message);
    }
}

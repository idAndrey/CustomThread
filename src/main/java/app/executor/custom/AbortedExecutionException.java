package app.executor.custom;

public class AbortedExecutionException extends RuntimeException {
    public AbortedExecutionException(String message) {
        super(message);
    }
}

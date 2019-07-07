package io.yooksi.jute.bash;

/**
 * Signals that an exception occurred while executing a bash operation.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class BashExecutionException extends Exception {

    public BashExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public BashExecutionException(String message) {
        super(message);
    }

    public BashExecutionException() {
        super("An exception occurred while executing a bash operation.");
    }
}

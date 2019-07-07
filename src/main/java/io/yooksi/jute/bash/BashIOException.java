package io.yooksi.jute.bash;

/**
 * Signals that an I/O exception occurred while executing a bash operation.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class BashIOException extends BashExecutionException {

    public BashIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public BashIOException(String message) {
        super(message);
    }

    public BashIOException() {
        super("An I/O error occurred while executing a bash operation.");
    }
}

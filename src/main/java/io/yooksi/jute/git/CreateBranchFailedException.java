package io.yooksi.jute.git;

import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Exception thrown when a Git branch is unable to be created.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CreateBranchFailedException extends GitAPIException {

    private static final long serialVersionUID = 1L;
    private static final String messageFormat = "Unable to create branch %s";

    public CreateBranchFailedException(String branch, String reason, Throwable cause) {
        super(String.format(messageFormat + ", " + reason + '.', branch), cause);
    }

    public CreateBranchFailedException(String branch, String reason) {
        this(branch, reason, null);
    }

    public CreateBranchFailedException(String branch, Throwable cause) {
        super(String.format(messageFormat + '.', branch), cause);
    }

    public CreateBranchFailedException(String branch) {
        this(branch, (Throwable) null);
    }
}

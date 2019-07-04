package io.yooksi.jute.git.bash;

import io.yooksi.commons.define.MethodsNotNull;
import io.yooksi.commons.logger.LibraryLogger;
import io.yooksi.commons.util.ArrayUtils;
import io.yooksi.commons.util.StringUtils;
import io.yooksi.commons.util.SystemUtils;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;

/**
 * This object represents a git bash command line application program.
 * It is capable of executing both git and regular bash commands.
 *
 * @see <a href=https://tiswww.case.edu/php/chet/bash/bashref.html>Bash Reference Manual</a>
 */
@MethodsNotNull
@SuppressWarnings({"unused", "WeakerAccess"})
public class GitBash {

    /**
     * Full git bash command line application name which represents an existing
     * program whose path is expected to be defined in environment variables.
     *
     * @see #GitBash()
     */
    private static final String CLI_APP_NAME =
            System.getProperty("bash.cli.name", "git-bash.exe");

    /**
     * Default {@code GitBash} instance available for public use.
     * @see #get()
     */
    private static final GitBash BASH = new GitBash();

    private final UnixPath path;
    private final String[] cmdPrefix;

    /**
     * Internal constructor only used by {@link #BASH}.
     *
     * @throws FileSystemNotFoundException if the constructor was unable
     * find the git bash CLI application path passed as VM argument through
     * system properties or statically defined in a OS environment variable.
     */
    private GitBash()  {
        /*
         * First try to find the path in system properties
         */
        String property = System.getProperty("bash.cli.path");
        if (property == null)
        {
            /* If the path was not found continue to search environment variables
             */
            final Path appPath = SystemUtils.getApplicationPath(CLI_APP_NAME, true);
            if (appPath != null && appPath.toFile().exists()) {
                path = UnixPath.get(appPath);
            }
            else {
                String error = "Unable to find git bash CLI application: " + CLI_APP_NAME;
                throw new FileSystemNotFoundException(error);
            }
        }
        else path = UnixPath.get(property);
        cmdPrefix = new String[] { path.toString(), "-i", "-c" };
    }

    /**
     * Create a new {@code GitBash} instance that represents a git bash
     * command line application program that resides under given path
     */
    public GitBash(Path gitBashPath) {

        path = UnixPath.get(gitBashPath);
        cmdPrefix = new String[] { path.toString(), "-i", "-c" };
    }

    /**
     * Execute a git bash command with {@code ProcessBuilder}.
     *
     * @throws NullPointerException if the return value of {@link BashCommand#toString()}
     * for the supplied {@code BashCommand} is {@code null}.
     *
     * @throws IOException if an I/O error occurs while starting {@code ProcessBuilder}.
     *
     * @throws InterruptedException if the current thread is interrupted by another thread
     * while it is waiting, then the wait is ended and this exception is thrown.
     */
    public void runCommand(BashCommand command) throws IOException, InterruptedException {

        String sCommand = StringUtils.quote(command.toString(), false);
        LibraryLogger.debug("Running git bash command: " + sCommand);
        ProcessBuilder pb = new ProcessBuilder(ArrayUtils.add(cmdPrefix, sCommand));
        pb.start().waitFor();
    }

    /**
     * Execute a script {@code BashCommand} for the given {@code BashScript}.
     */
    public void runBashScript(BashScript script) {

        try {
            String command = StringUtils.quote(script.getPath().toString(), true);
            runCommand(new BashCommand(BashCommand.Type.SCRIPT, command));
        }
        catch (IOException | InterruptedException e)
        {
            String log = "An exception occurred while trying to run bash script ";
            LibraryLogger.error(log + script.getFile().getName(), e);
        }
    }

    /**
     * @return default {@code GitBash} instance.
     */
    public static GitBash get() {
        return BASH;
    }

    public UnixPath getPath() {
        return path;
    }
}


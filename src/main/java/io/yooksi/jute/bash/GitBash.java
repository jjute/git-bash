package io.yooksi.jute.bash;

import io.yooksi.jute.commons.define.MethodsNotNull;
import io.yooksi.jute.commons.logger.LibraryLogger;
import io.yooksi.jute.commons.util.SystemUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>
 *     This object represents a git bash command line application program that
 *     is capable of executing both git and regular bash commands. The class comes
 *     with a default instance which is statically initialized for ease of access.
 * <p>
 *     Constructor will perform <i>platform-specific</i> operations to determine the
 *     path to the Bash executable used by Apache {@code CommandLine} to initialize
 *     and {@code DefaultExecutor} to execute commands. Currently only Windows and
 *     Unix operating system platforms are fully tested.
 * <ul>
 *     <li>On <i>Windows</i> the constructor will search for the application path in
 *     environment variables and if not found a {@code RuntimeException} will be thrown.</li>
 *     <li>On <i>Unix</i> {@code bash} path will be used to execute commands.</li>
 * </ul>
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

    /** Default {@code GitBash} instance available for public use. */
    private static final GitBash BASH = new GitBash();

    /** Will be{@code true} if the current platform is a {@code UNIX} like system. */
    private final boolean isOsUnix;

    /** Path to the Bash executable application. */
    private final Path executable;

    /**
     * Internal constructor only used by {@link #BASH}.
     *
     * @throws FileSystemNotFoundException when the git bash command line
     * application path under {@link #CLI_APP_NAME} was unable to be resolved.
     */
    private GitBash() throws FileSystemNotFoundException {

        String execPath = "bash";
        isOsUnix = SystemUtils.IS_OS_UNIX;

        if (!isOsUnix)
        {
            /* First try to find the path in system properties
             */
            execPath = System.getProperty("bash.cli.path");
            if (execPath == null)
            {
                /* If the path was not found continue to search environment variables
                 */
                final Path appPath = SystemUtils.getApplicationPath(CLI_APP_NAME, true);
                if (appPath != null && appPath.toFile().exists()) {
                    executable = appPath; return;
                }
                else {
                    String error = "Unable to find git bash CLI application: " + CLI_APP_NAME;
                    throw new FileSystemNotFoundException(error);
                }
            }
        }
        executable = Paths.get(execPath);
    }

    /**
     * Create a new {@code GitBash} instance that represents a git bash
     * command line application program that resides under given path
     * 
     * @throws UnsupportedOperationException if the current platform is a <i>Unix-based</i> system.
     */
    public GitBash(Path gitBashPath) {

        if (SystemUtils.IS_OS_UNIX) {
            throw new UnsupportedOperationException("This operation is not supported on Unix");
        }
        isOsUnix = false;
        executable = gitBashPath;
    }

    /**
     * Execute a Bash command with {@code DefaultExecutor}.
     *
     * @return process exit value
     *
     * @throws NullPointerException if the return value of {@link BashCommand#toString()}
     * for the supplied {@code BashCommand} is {@code null}.
     *
     * @throws IOException if an I/O error occurs while executing command.
     * @throws ExecuteException execution of subprocess failed or the subprocess
     *                          returned a exit value indicating a failure
     *
     * @see DefaultExecutor#execute(CommandLine)
     */
    @SuppressWarnings("UnusedReturnValue")
    public int runCommand(BashCommand command) throws IOException, ExecuteException {

        String sCommand = command.toString();
        LibraryLogger.debug("Running git bash command: " + sCommand);

        CommandLine cmdLine = new CommandLine(executable.toString());
        cmdLine.addArguments(new String[] { "-c", sCommand }, false);

        DefaultExecutor executor = new DefaultExecutor();
        return executor.execute(cmdLine);
    }

    /**
     * Execute a script {@code BashCommand} for the given {@code BashScript}.
     *
     * @throws BashIOException if an I/O error occurs while starting {@code ProcessBuilder}.
     */
    public void runBashScript(BashScript script) throws BashIOException  {

        try {
            runCommand(new BashCommand(BashCommand.Type.SCRIPT, script.getPath().toString()));
        }
        catch (IOException e)
        {
            String log = "An exception occurred while trying to run bash script ";
            throw new BashIOException(log + script.getFile().getName(), e);
        }
    }

    /**
     * @return default {@code GitBash} instance.
     */
    public static GitBash get() {
        return BASH;
    }
}
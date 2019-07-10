package io.yooksi.jute.bash;

import io.yooksi.jute.commons.define.MethodsNotNull;
import io.yooksi.jute.commons.logger.LibraryLogger;
import io.yooksi.jute.commons.util.ArrayUtils;
import io.yooksi.jute.commons.util.StringUtils;
import io.yooksi.jute.commons.util.SystemUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * <p>
 *     This object represents a git bash command line application program that
 *     is capable of executing both git and regular bash commands. The class comes
 *     with a default instance which is statically initialized for ease of access.
 * <p>
 *     Constructor will perform <i>platform-specific</i> operations to determine the
 *     <i>platform-dependent</i> command prefix used in {@code ProcessBuilder} to
 *     execute commands. Currently only Windows and Unix are supported.
 * <ul>
 *     <li>On <i>Windows</i> the constructor will search for the application path in
 *     environment variables and if not found it will use {@code command prompt}.</li>
 *     <li>On <i>Unix</i> a {@code sh} prefix will be used to execute commands.</li>
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

    /**
     * Default {@code GitBash} instance available for public use.
     * @see #get()
     */
    private static final GitBash BASH = new GitBash();

    /**
     * Will be{@code true} if the current platform is a {@code UNIX} like system.
     */
    private final boolean isOsUnix;
    /**
     * Used to set the first {@code ProcessBuilder} command argument.
     */
    private final String[] cmdPrefix;

    /* Internal constructor only used by {@link #BASH}. */
    private GitBash() {

        cmdPrefix = new String[] { "", "-c" };
        isOsUnix = SystemUtils.IS_OS_UNIX;

        if (!isOsUnix)
        {
            /* First try to find the path in system properties
             */
            cmdPrefix[0] = System.getProperty("bash.cli.path");
            if (cmdPrefix[0] == null)
            {
                /* If the path was not found continue to search environment variables
                 */
                final Path appPath = SystemUtils.getApplicationPath(CLI_APP_NAME, true);
                if (appPath != null && appPath.toFile().exists()) {
                    cmdPrefix[0] = appPath.toString();
                }
                else {
                    LibraryLogger.warn("Unable to find git bash CLI application %s, " +
                            "defaulting to cmd.exe", CLI_APP_NAME);

                    cmdPrefix[0] = "cmd.exe";
                }
            }
        }
        else cmdPrefix[0] = "sh";
    }

    /**
     * Create a new {@code GitBash} instance that represents a git bash
     * command line application program that resides under given path
     * 
     * @throws IllegalStateException if the current platform is a <i>Unix-based</i> system.
     */
    public GitBash(Path gitBashPath) {

        if (SystemUtils.IS_OS_UNIX) {
            throw new IllegalStateException("This operation is not supported on Unix");
        }
        isOsUnix = false;
        cmdPrefix = new String[] { gitBashPath.toString(), "-c" };
    }

    /**
     * Execute a git bash command with {@code ProcessBuilder}.
     *
     * @throws NullPointerException if the return value of {@link BashCommand#toString()}
     * for the supplied {@code BashCommand} is {@code null}.
     *
     * @throws IOException if an I/O error occurs while starting {@code ProcessBuilder}.
     *
     * @throws InterruptedException if the current thread is interrupted by another thread while
     *                              it is waiting, then the wait is ended and this exception is thrown.
     */
    public void runCommand(BashCommand command) throws IOException, InterruptedException {

        String sCommand = isOsUnix ? command.toString() : StringUtils.quote(command.toString(), false);
        LibraryLogger.debug("Running git bash command: " + sCommand);

        ProcessBuilder pb = new ProcessBuilder(ArrayUtils.add(cmdPrefix, sCommand));
        pb.start().waitFor();
    }

    /**
     * Execute a script {@code BashCommand} for the given {@code BashScript}.
     *
     * @throws BashIOException if an I/O error occurs while starting {@code ProcessBuilder}.
     * @throws BashExecutionException if the current thread is interrupted by another thread while
     *                                it is waiting, then the wait is ended and this exception is thrown
     */
    public void runBashScript(BashScript script) throws BashExecutionException  {

        try {
            runCommand(new BashCommand(BashCommand.Type.SCRIPT, script.getPath().toString()));
        }
        catch (IOException | InterruptedException e)
        {
            String log = "An exception occurred while trying to run bash script " + script.getFile().getName();
            throw e instanceof IOException ? new BashIOException(log , e) : new BashExecutionException(log, e);
        }
    }

    /**
     * @return default {@code GitBash} instance.
     */
    public static GitBash get() {
        return BASH;
    }
}
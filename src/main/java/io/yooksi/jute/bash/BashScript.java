package io.yooksi.jute.bash;

import io.yooksi.commons.define.IBuilder;
import io.yooksi.commons.define.MethodsNotNull;
import io.yooksi.commons.util.ArrayUtils;
import io.yooksi.commons.util.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * This object represents a programmatically constructed bash script.
 *
 * Note that the script is not guaranteed to exist in a physical file
 * nor is it guaranteed to actually contain commands.
 *
 * @see <a href=https://tiswww.case.edu/php/chet/bash/bashref.html#Shell-Scripts>
 *     Bash Reference Manual: Shell Scripts</a>
 */
@MethodsNotNull
@SuppressWarnings("unused")
public class BashScript {

    private final UnixPath path;
    private final java.io.File file;
    private final List<String> commands;

    /**
     * @param path bash script file path
     * @param commands list of bash commands
     * @param write whether to write the commands to file
     *
     * @throws RuntimeException when an I/O exception occurs while writing to file.
     *         We are using a {@code RuntimeException} here to get around interface contracts.
     */
    private BashScript(UnixPath path, List<String> commands, boolean write) throws RuntimeException {

        try {
            this.path = path;
            this.file = new java.io.File(path.toString());
            this.commands = commands;

            if (write) {
                if (!file.exists() && !file.createNewFile()) {
                    throw new IOException();
                }
                FileUtils.writeLines(file, commands);
            }
        }
        catch (IOException e) {
            String log = "Failed to create new BashScript file " + path.toString();
            throw new RuntimeException(new BashIOException(log, e));
        }
    }

    /**
     * Use one of the available {@code create} methods to create a new {@code Builder}
     * instance, then chain call available class methods to construct each command.
     * When all commands have been constructed use {@link #build()} to finalize all
     * information and build a new {@code BashScript} instance.
     */
    public static class Builder implements IBuilder<BashScript> {

        private final Path scriptPath;
        private final @Nullable RedirectOutput redirect;

        private final boolean write;

        private final StringBuilder command = new StringBuilder();
        private final List<String> lines = new java.util.ArrayList<>();

        /**
         * @param script abstract path to the bash script file
         * @param redirect instructions on how to redirect command output
         * @param write whether to write the commands to file
         */
        private Builder(Path script, @Nullable RedirectOutput redirect, boolean write) {

            this.scriptPath = script;
            this.redirect = redirect;
            this.write = write;
        }

        /**
         * Print the given text to console or file <i>(if redirection is enabled)</i>.
         * @see #next(RedirectOutput)
         */
        public Builder echo(String text) {
            return next().appendQuoted("echo", text);
        }

        /**
         * Redirect the current command to a file. Note that this method will have
         * no effect if it's not called on an active command. In other words it will
         * do nothing if the {@code command} field is empty.
         *
         * @see #next(RedirectOutput)
         */
        public Builder toFile(RedirectOutput redirect) {
            return next(redirect);
        }

        /**
         * <p>Append a single {@code BashCommand} to this script.</p>
         * <i>Note that this process will reset the current command.</i>
         */
        public Builder appendCmd(BashCommand cmd) {
            return next().appendCmd(cmd.toString());
        }
        /**
         * <p>Append multiple bash commands to this script.</p>
         * <i>Note that this process will reset the current command.</i>
         */
        public Builder appendCmds(BashCommand cmd, BashCommand... more) {

            Arrays.stream(ArrayUtils.prepend(cmd, more)).forEach(this::appendCmd);
            return next();
        }

        /**
         * Construct the current command from the given parameters.
         * <p><i>
         *     Note that this method will overwrite the current command if it is not
         *     called as the first element in the chain for every command being constructed.
         * </i></p>
         *
         * @param cmd first string to append to the current command
         * @param args array of strings to append in natural order
         */
        private Builder appendCmd(String cmd, String... args) {

            if (command.length() > 0) {
                resetCommand();
            }
            command.append(cmd);
            return appendArgs(args);
        }
        /**
         * Construct the current command from the given parameters and close
         * the arguments in double quotation marks. Note that this method will
         * overwrite the current command if it is not called as the first element
         * in the chain for every command being constructed.
         *
         * @param cmd first string to append to the current command
         * @param args array of strings to append in natural order
         */
        @SuppressWarnings("SameParameterValue")
        private Builder appendQuoted(String cmd, String... args) {

            if (command.length() > 0) {
                resetCommand();
            }
            command.append(cmd).append(' ').append('\"');
            for (String arg : args) {
                command.append(arg).append(' ');
            }
            command.setCharAt(command.length() - 1, '\"');
            return this;
        }
        /**
         * Append the given arguments to the current command,
         * separating the command and each argument with a single whitespace.
         */
        private Builder appendArgs(String...args) {

            for (String arg : args) {
                command.append(' ').append(arg);
            }
            return this;
        }

        /**
         * Finalize, store and reset the current command. The command is finalized
         * by appending the given output redirection arguments <i>(if not null)</i>
         * to the current command before storing and resetting it.
         *
         * @param redirect command output redirection data
         */
        private Builder next(@Nullable RedirectOutput redirect) {

            if (command.length() > 0)
            {
                if (redirect != null)
                {
                    String operator = !lines.isEmpty() && redirect.type == RedirectOutput.Type.OVERWRITE ?
                            RedirectOutput.Type.APPEND.value : redirect.type.value;

                    appendArgs(operator, redirect.path.toString());
                }
                lines.add(command.toString());
                resetCommand();
            }
            return this;
        }

        /**
         * Finalize, store and reset the current command.
         * @see #next(RedirectOutput)
         */
        private Builder next() {
            return next(redirect);
        }

        private void resetCommand() {
            command.delete(0, command.length());
        }

        @Override
        public BashScript build() {
            next(); return new BashScript(UnixPath.get(scriptPath), lines, write);
        }
    }

    /**
     * @param path abstract location of the script file
     * @param write whether to write the commands to file
     * @return new {@code BashScript} under given path.
     */
    public static Builder create(Path path, boolean write) {
        return new Builder(path, null, write);
    }
    /**
     * @param path abstract location of the script file
     * @param redirect how to redirect <b>all</b> commands
     * @param write whether to write the commands to file
     * @return new {@code BashScript} under given path.
     */
    public static Builder create(Path path, RedirectOutput redirect, boolean write) {
        return new Builder(path, redirect, write);
    }

    public UnixPath getPath() {
        return path;
    }
    public java.io.File getFile() {
        return file;
    }
    public List<String> getCommands() {
        return commands;
    }
}

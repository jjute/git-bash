package io.yooksi.jute.git.bash;

import io.yooksi.jute.git.GitCommand;

/**
 * This object represents a bash command of a specific {@link Type}.
 *
 * @see <a href=https://tiswww.case.edu/php/chet/bash/bashref.html#Shell-Commands>
 *      Bash Reference Manual: Shell Commands</a>
 */
public class BashCommand {

    public enum Type {
        /**
         * When this command is executed bash will read and execute a script stored in a file
         * then exit. It sets the special parameter 0 to the name of the file, rather than the
         * name of the shell, and the positional parameters are set to the remaining arguments,
         * if any are given. If no additional arguments are supplied, the positional parameters
         * are unset and the positional parameters are set to the remaining arguments.
         */
        SCRIPT("bash"),

        /**
         * This command type is used by every {@link GitCommand}. It requires Git version
         * control system to be installed and appropriately added to the system path.
         */
        GIT("git");

        private final String name;
        Type(String name) {
            this.name = name;
        }
    }

    private final Type type;
    private final String command;

    protected BashCommand(Type type, String command) {
        this.command = command;
        this.type = type;
    }

    /**
     * @return a {@code String} representation of this command.
     */
    public String toString() {
        return !type.name.isEmpty() ? type.name + ' ' + command : command;

    }
}

package io.yooksi.jute.bash;

import io.yooksi.jute.commons.define.MethodsNotNull;

/**
 * Bash command argument for redirecting {@code stdout} to a local file.
 * Each object is created for a specific redirection type and local file path.
 *
 * @see <a href=https://www.tldp.org/LDP/abs/html/io-redirection.html>
 *     Advanced Bash-Scripting Guide: I/O Redirection</a>
 */
@MethodsNotNull
@SuppressWarnings("unused")
public class RedirectOutput {

    public enum Type {
        /**
         * Creates the file if not present, otherwise overwrites it.
         */
        OVERWRITE(">"),
        /**
         * Creates the file if not present, otherwise appends to it.
         */
        APPEND(">>");

        final String value;
        Type(String value) {
            this.value = value;
        }

        private RedirectOutput create(UnixPath path) {
            return new RedirectOutput(path, this);
        }
    }

    final UnixPath path;
    final Type type;

    private RedirectOutput(UnixPath path, Type type) {
        this.path = path; this.type = type;
    }

    /**
     * @return {@code RedirectOutput} that will overwrite the file.
     * @see Type#OVERWRITE
     */
    public static RedirectOutput overwrite(UnixPath path) {
        return Type.OVERWRITE.create(path);
    }
    /**
     * @return {@code RedirectOutput} that will append to file.
     * @see Type#APPEND
     */
    public static RedirectOutput appendTo(UnixPath path) {
        return Type.APPEND.create(path);
    }
}

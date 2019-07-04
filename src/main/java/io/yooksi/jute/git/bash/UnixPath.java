package io.yooksi.jute.git.bash;

import io.yooksi.commons.define.MethodsNotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This object represents a Unix-style path that uses <i>forward slashes</i>. This contrasts with paths
 * in MS-DOS and similar operating systems (such as FreeDOS) and the Microsoft Windows operating systems,
 * in which directories and files are separated  with backslashes. The backslash is an upward-to-the-left
 * sloping straight line character that is a mirror image of the forward slash.
 *
 * <ul style="list-style-type:none">
 *     <li>Unix path: {@code C:/path/to/file}</li>
 *     <li>Windows path: {@code C:\path\to\file}</li>
 * </ul>
 *
 * @see <a href=http://www.linfo.org/forward_slash.html>Forward Slash Definition</a>
 */
@MethodsNotNull
public class UnixPath {

    private final String path;

    private UnixPath(Path path) {
        this.path = convert(path);
    }

    /**
     * Convert a given {@code File} path into a <i>Unix-style</i> path.
     */
    public static UnixPath get(File file) {
        return new UnixPath(file.toPath());
    }
    /**
     * Convert a given {@code Path} into a <i>Unix-style</i> path.
     */
    public static UnixPath get(Path path) {
        return new UnixPath(path);
    }
    /**
     * Convert a given path into a <i>Unix-style</i> path.
     *
     * @throws java.nio.file.InvalidPathException
     * if the path {@code String} cannot be converted to a {@code Path}
     */
    public static UnixPath get(String path) {
        return new UnixPath(Paths.get(path));
    }

    /**
     * Convert the given path to a standard Java {@code Path}.
     * Use this method when you don't want to instantiate a {@code UnixPath}
     * object and just want a quick conversion to a <i>Unix-style</i> path.
     */
    public static String convert(Path path) {
        return path.toString().replace("\\", "/");
    }
    /**
     * Convert this path to a standard Java {@code Path}.
     */
    public Path convert() {
        return Paths.get(path);
    }
    /**
     * @return a {@code String} representation of this path.
     */
    @Override
    public String toString() {
        return path;
    }
}

package io.yooksi.jute.git;

import io.yooksi.jute.commons.define.IBuilder;
import io.yooksi.jute.commons.util.ArrayUtils;

import java.nio.file.Path;

/**
 * Show changes between the working tree and the index or a tree,
 * changes between the index and a tree, changes between two trees,
 * changes between two blob objects, or changes between two files on disk.
 */
@SuppressWarnings("unused")
public class DiffCommand extends GitCommand {

    private static final String FORMAT = "diff %opts ";

    private DiffCommand(String[] args, GitCLOption... options) {
        super(FORMAT + String.join(" ", args), options);
    }
    private DiffCommand(String arg, GitCLOption[] options) {
        this(new String[] {arg}, options);
    }
    private DiffCommand(String arg, String... more) {
        this(ArrayUtils.prepend(arg, more), GitCLOption.NONE);
    }

    /**
     * View the changes you made relative to the index (staging area for the next commit).
     * In other words, the differences are what you could tell Git to further
     * add to the index but you still haven’t.
     */
     public static DiffCommand relativeToIndex(Path path) {
         return new DiffCommand(path.toString());
    }

    /**
     * Compare the given two paths on the filesystem. You can omit the {@code noIndex} option when
     * running the command in a working tree controlled by Git and at least one of the paths points
     * outside the working tree, or when running the command outside a working tree controlled by Git.
     *
     * @param noIndex whether to include non-indexed files
     */
    public static DiffCommand comparePaths(Path path1, Path path2, boolean noIndex) {
         return new DiffCommand(noIndex ? "--no-index" : "", path1.toString(), path2.toString());
    }

    /**
     * Use {@link #create()} method to create a new {@code Builder} instance,
     * then chain call available class methods to configure the object.
     * When ready use {@link #build()} method to build a new {@code DiffCommand} instance.
     */
    public static class Builder implements IBuilder<DiffCommand> {

        @Override
        public DiffCommand build() {
            return null;
        }
    }

    /**
     * @return a new {@code Builder} instance intended to be used to
     *         build a custom configured {@code DiffCommand} instance.
     */
    public static Builder create() {
        return new Builder();
    }

    @Override
    public String toString() {
        return null;
    }
}

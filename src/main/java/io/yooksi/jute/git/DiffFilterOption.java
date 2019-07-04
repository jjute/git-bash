package io.yooksi.jute.git;

import io.yooksi.commons.define.IBuilder;
import org.jetbrains.annotations.Contract;

import java.util.Map;

/**
 * <p>
 *     When this filter added to a {@code Diff} command the output value will contain only
 *     files that match the contained filter option types. Any combination of filter types
 *     <i>(including none)</i> can be used and duplicate types will be discarded.
 * </p>
 * <i>
 *     Note that not all diffs can feature all types. For instance, diffs from the index
 *     to the working tree can never have Added entries (because the set of paths included
 *     in the diff is limited by hat is in the index). Similarly, copied and renamed entries
 *     cannot appear if detection for those types is disabled.
 * </i>
 * @see <a href="https://git-scm.com/docs/git-diff#Documentation/git-diff.txt---diff-filterACDMRTUXB82308203">
 *      Official GIT documentation</a>
 */
@SuppressWarnings("unused")
public class DiffFilterOption extends ParamCLOption {

    private static final String FORMAT = "--diff-filter=[%s]";

    public enum Type {

        ADDED( 'A'), COPIED('C'), DELETED('D'), MODIFIED('M'), RENAMED('R'),
        TYPE_CHANGED('T'), UNMERGED('U'), UNKNOWN('X'), BROKEN('B');

        private final char value;
        Type(char t) { this.value = t; }
    }

    public static final DiffFilterOption ADDED = new DiffFilterOption(Type.ADDED);
    public static final DiffFilterOption COPIED = new DiffFilterOption(Type.COPIED);
    public static final DiffFilterOption DELETED = new DiffFilterOption(Type.DELETED);
    public static final DiffFilterOption MODIFIED = new DiffFilterOption(Type.MODIFIED);
    public static final DiffFilterOption RENAMED = new DiffFilterOption(Type.RENAMED);
    public static final DiffFilterOption TYPE_CHANGED = new DiffFilterOption(Type.TYPE_CHANGED);
    public static final DiffFilterOption UNMERGED = new DiffFilterOption(Type.UNMERGED);
    public static final DiffFilterOption UNKNOWN = new DiffFilterOption(Type.UNKNOWN);
    public static final DiffFilterOption BROKEN = new DiffFilterOption(Type.BROKEN);

    /**
     * Internal data storage containing option types mapped to a boolean value that
     * designates types as either inclusive ({@code true}) or exclusive ({@code false}).
     */
    private final Map<Type, Boolean> map = new java.util.HashMap<>();

    /**
     * When this variable is set to {@code true} all paths are selected
     * if there is any file that matches other criteria in the comparison;
     * if there is no file that matches other criteria, nothing is selected.
     */
    private final boolean allOrNone;

    /**
     * Use {@link #create()} method to create a new {@code Builder} instance,
     * then chain call available class methods to configure the object.
     * When all configurations have been setup use {@link #build()}
     * method to build a new {@code DiffFilterOption} instance.
     */
    public static class Builder implements IBuilder<DiffFilterOption> {

        private final Map<Type, Boolean> map = new java.util.HashMap<>();
        private boolean allOrNone = false;
        /**
         * Designate types of files to include in a diff.
         */
        @Contract("_ -> this")
        public Builder include(Type... types) {
            for (Type type : types) {
                map.put(type, true);
            }
            return this;
        }
        /**
         * Designate types of files to exclude from a diff.
         */
        @Contract("_ -> this")
        public Builder exclude(Type... types) {
            for (Type type : types) {
                map.put(type, false);
            }
            return this;
        }
        /**
         * Make operation absolute in that it will include <b>all files</b>
         * if there is at least one file that matches other criteria.
         * Read {@link DiffFilterOption#allOrNone} documentation for more information
         */
        @Contract("_ -> this")
        public Builder setAllOrNone(boolean state) {
            allOrNone = state;
            return this;
        }
        @Override
        public DiffFilterOption build() {

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Type, Boolean> entry : map.entrySet())
            {
                char key = entry.getKey().value;
                sb.append(entry.getValue() ? key : Character.toLowerCase(key));
            }
            String params = sb.toString() + (allOrNone ? "*" : "");
            return new DiffFilterOption(params, map, allOrNone);
        }
    }
    /* This constructor is only intended for use by static final options */
    private DiffFilterOption(Type type) {

        super(FORMAT, String.valueOf(type.value));
        this.map.put(type, true);
        this.allOrNone = false;
    }
    /* This constructor is only intended for use by option builder */
    private DiffFilterOption(String params, Map<Type, Boolean> data, boolean allOrNone) {

        super(FORMAT, params);
        data.forEach(this.map::put);
        this.allOrNone = allOrNone;
    }

    /**
     * @return a new {@code Builder} instance intended to be used to
     *         build a custom configured {@code DiffFilterOption} instance.
     */
    public static Builder create() {
        return new Builder();
    }

    @Override
    @Contract(pure = true)
    public String getValue() {
        return super.getValue().replaceAll("[\\[\\]]", "");
    }
}

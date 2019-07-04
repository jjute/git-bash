package io.yooksi.jute.git;

import io.yooksi.jute.bash.BashCommand;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;

@SuppressWarnings("unused")
public class GitCommand extends BashCommand {

    public static final GitCommand VERSION = new GitCommand(BasicCLOption.VERSION.toString());

    private final GitCLOption[] options;

    GitCommand(String format, String[] args, GitCLOption... options) {
        super(Type.GIT, formatCommand(format, args, options));
        this.options = options;
    }

    GitCommand(String cmd, GitCLOption... options) {
        super(Type.GIT, cmd.replace("%opts", consolidateOptions(options)));
        this.options = options;
    }

    private static String formatCommand(String format, String[] args, GitCLOption... options) {

        format = format.replace("%opts", consolidateOptions(options));

        for (GitCLOption option : options) {
            format = format.replaceFirst("%opt", option.toString());
        }
        return args.length > 0 ? String.format(format, (Object[]) args) : format;
    }

    private static String consolidateOptions(GitCLOption... options) {

        if (options.length > 0)
        {
            StringBuilder sb = new StringBuilder();
            Arrays.stream(options).forEach(o -> sb.append(o.toString()).append(" "));
            return sb.toString();
        }
        else return "";
    }

    @Contract(pure = true)
    public GitCLOption[] getOptions() {
        return options;
    }
}

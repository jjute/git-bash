package io.yooksi.jute.git;

@SuppressWarnings({"unused", "WeakerAccess"})
public class BasicCLOption extends SimpleCLOption {

    public static final BasicCLOption VERSION = new BasicCLOption("--version");
    public static final BasicCLOption HELP = new BasicCLOption("--help");

    public BasicCLOption(String value) {
        super(value);
    }
}

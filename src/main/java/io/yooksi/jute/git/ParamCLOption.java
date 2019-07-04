package io.yooksi.jute.git;

import javafx.util.Pair;
import org.apache.commons.lang3.ArrayUtils;

import javax.validation.constraints.NotEmpty;
import java.util.Arrays;

public abstract class ParamCLOption implements GitCLOption {

    private final Pair<String, String> data;
    private final String option;

    @SuppressWarnings("SameParameterValue")
    ParamCLOption(String format, String param, String...more) throws IllegalArgumentException {

        String[] params = ArrayUtils.addAll(new String[] { param }, more);
        String[] sOption = String.format(format, (Object[]) params).split("=");
        if (sOption.length == 2) {
            this.data = new Pair<>(sOption[0], sOption[1]);
            this.option = sOption[0] + "=" + sOption[1];
        }
        else {
            String log = "Unable to format GCL option with given parameters: \"%s\" using format: \"%s\"";
            throw new IllegalArgumentException(String.format(log, Arrays.toString(params), format));
        }
    }

    @Override
    public @NotEmpty String getDesignation() {
        return data.getKey();
    }

    @Override
    public String getValue() {
        return data.getValue();
    }

    @Override
    public String toString() {
        return option;
    }
}

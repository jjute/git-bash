package io.yooksi.jute.git;

import io.yooksi.jute.commons.define.MethodsNotNull;

import javax.validation.constraints.NotEmpty;

@MethodsNotNull
@SuppressWarnings("WeakerAccess")
public abstract class SimpleCLOption implements GitCLOption {

    private final String option;

    public SimpleCLOption(String value) {
        this.option = value;
    }

    @Override
    public @NotEmpty String getDesignation() {
        return toString();
    }

    @Override
    public String getValue() {
        return "";
    }

    @Override
    public String toString() {
        return option;
    }
}

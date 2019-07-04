package io.yooksi.jute.git;

import org.jetbrains.annotations.Contract;
import javax.validation.constraints.NotEmpty;

public interface GitCLOption {

    GitCLOption[] NONE = null;

    @Contract(pure = true)
    @NotEmpty String getDesignation();

    @Contract(pure = true)
    String getValue();

    @Contract(pure = true)
    String toString();
}

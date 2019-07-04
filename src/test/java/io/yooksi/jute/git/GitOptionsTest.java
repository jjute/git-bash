package io.yooksi.jute.git;

import io.yooksi.commons.git.DiffFilterOption;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

@SuppressWarnings("WeakerAccess")
public class GitOptionsTest {

    @Test
    public void createDiffFilterOptionsTest() {

        DiffFilterOption option = DiffFilterOption.create().build();
        Assertions.assertEquals("--diff-filter", option.getDesignation());
        Assertions.assertTrue(option.getValue().isEmpty());

        DiffFilterOption.Type[] types = { DiffFilterOption.Type.ADDED, DiffFilterOption.Type.COPIED };
        option = DiffFilterOption.create().include(types).build();
        shouldContainDiffFilterOptions(option.getValue(), 'A', 'C');

        option = DiffFilterOption.create().include(types).exclude(DiffFilterOption.Type.RENAMED).build();
        shouldContainDiffFilterOptions(option.getValue(), 'A', 'C', 'r');

        option = DiffFilterOption.create().include(types).setAllOrNone(true).build();
        shouldContainDiffFilterOptions(option.getValue(), 'A', 'C', '*');
    }

    @TestOnly
    public void shouldContainDiffFilterOptions(String value, Character... options) {
        Arrays.stream(options).forEach(o -> Assertions.assertTrue(value.contains(Character.toString(o))));
    }
}

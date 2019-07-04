package io.yooksi.jute.git;

import io.yooksi.jute.git.bash.*;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("WeakerAccess")
public class GitCommandTest {

    @Test
    public void testGitVersionCommand() throws IOException {

        Path scriptPath = Paths.get("testScript.sh");
        UnixPath logPath = UnixPath.get("logs/testScript.log");
        RedirectOutput redirect = RedirectOutput.overwrite(logPath);

        BashScript script = BashScript.create(scriptPath, redirect, true).appendCmd(GitCommand.VERSION).build();
        GitBash.get().runBashScript(script);

        String output = FileUtils.readFileToString(logPath.convert().toFile(), Charset.defaultCharset());
        Assertions.assertTrue(output.contains("git version"));
    }
}

package io.yooksi.jute.git;

import io.yooksi.commons.util.FileUtils;
import io.yooksi.commons.util.StringUtils;
import io.yooksi.jute.bash.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@SuppressWarnings({"unused", "WeakerAccess"})
public class BashTests {

    private static final File logFile = new File("bashTest.log");

    @Test
    public void runBashScriptEchoSingleLineTest() throws BashExecutionException, IOException {

        String logText = "Hello World";
        Path scriptPath = Paths.get("testScript.sh");
        UnixPath logPath = UnixPath.get("logs/testScript.log");

        BashScript script = BashScript.create(scriptPath, true)
                .echo(logText).toFile(RedirectOutput.overwrite(logPath)).build();

        String output = runBashScript(logPath, script);
        Assertions.assertEquals(logText, output.trim());
    }

    @Test
    public void runBashScriptEchoMultiLine1Test() throws BashExecutionException, IOException {

        String[] logText = { "First line ", "Second line ", "Third line " };
        Path scriptPath = Paths.get("testScript.sh");
        UnixPath logPath = UnixPath.get("logs/testScript.log");

        BashScript script = BashScript.create(scriptPath, RedirectOutput.overwrite(logPath), true)
                .echo(logText[0]).echo(logText[1]).echo(logText[2]).build();

        String output = StringUtils.normalizeEOL(runBashScript(logPath, script));

        File tempTxt = FileUtils.getTempFile("compare.txt");
        FileUtils.writeLines(tempTxt, Arrays.asList(logText));
        Assertions.assertEquals(FileUtils.readFileToString(tempTxt, Charset.defaultCharset()).trim(), output.trim());
    }

    private String runBashScript(UnixPath logPath, BashScript script) throws BashExecutionException, IOException {

        GitBash.get().runBashScript(script);
        File logFile = new File(logPath.toString());
        return FileUtils.readFileToString(logFile, Charset.defaultCharset());
    }

    @Test
    public void unixPathTest() {

        Path winPath = Paths.get("C:\\simple\\windows\\path");
        UnixPath unixPath = UnixPath.get(winPath);

        Assertions.assertFalse(unixPath.toString().contains("\\"));
        Assertions.assertEquals(winPath.toString(), unixPath.convert().toString());
    }
}

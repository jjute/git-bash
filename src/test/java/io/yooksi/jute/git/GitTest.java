package io.yooksi.jute.git;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"unused", "WeakerAccess"})
public class GitTest {

    private static final Charset charset = Charset.defaultCharset();
    private static final List<File> sampleFiles = new java.util.ArrayList<>();

    private static Path path;
    private static Git git;

    @Test @Order(1)
    public void openGitRepositoryTest() {

        Assertions.assertDoesNotThrow(this::openGitRepository);
    }
    private void openGitRepository() throws IOException {

        git = Git.openRepository();
        git = Git.openRepository(Paths.get(".git"));
    }

    @Test @Order(2)
    public void initializeGitRepositoryTest() {

        Assertions.assertDoesNotThrow(this::initializeGitRepository);
    }
    private void initializeGitRepository() throws IOException, GitAPIException {

        path = Paths.get("testRepo");
        git = Git.initRepository(path);
//        git = Git.initRepository(path.resolve(".git"));
    }

    @Test @Order(3)
    public void commitFilesTest() throws IOException, GitAPIException {

        Assertions.assertTrue(git.diff().call().isEmpty());
        Assertions.assertThrows(NoHeadException.class, git::getLastCommitMessage);

        File sampleFile = path.resolve("sample1.txt").toFile();
        Assertions.assertTrue(sampleFile.createNewFile());

        Assertions.assertFalse(git.diff().call().isEmpty());

        FileUtils.write(sampleFile, "sample text", charset);
        git.add(sampleFile.toPath());

        String commitMessage = "Create first sample text file";
        git.commit(commitMessage, false);

        Assertions.assertEquals(commitMessage, git.getLastCommitMessage());
        Assertions.assertTrue(git.diff().call().isEmpty());
        Assertions.assertTrue(sampleFile.delete());
    }

    @Test @Order(4)
    public void rewindHeadTest() throws GitAPIException, IOException {

        createAndCommitSampleFile("sample2.txt");
        createAndCommitSampleFile("sample3.txt");

        Ref head = git.rewind(2, ResetCommand.ResetType.HARD);
        List<RevCommit> commits = git.getAllCommits();

        Assertions.assertEquals(1, commits.size());
        Assertions.assertEquals(git.getHead().getName(), head.getName());
    }

    @Test @Order(4)
    public void listAndPopStashEntriesTest() throws GitAPIException, IOException {

        Assertions.assertEquals(0, git.stashList().call().size());

        List<RevCommit> stashList = new java.util.ArrayList<>();

        File sampleFile = createAndCommitSampleFile("sample2.txt", "sample text");
        FileUtils.write(sampleFile, "first change", charset, false);
        stashList.add(git.stashChanges());

        FileUtils.write(sampleFile, "second change", charset, false);
        stashList.add(git.stashChanges());

        FileUtils.write(sampleFile, "third change", charset, false);
        stashList.add(git.stashChanges());

        List<RevCommit> expectedStashList = stashList.subList(0, stashList.size());
        java.util.Collections.reverse(expectedStashList);

        List<RevCommit> actualStashList = git.getStashList();
        Assertions.assertEquals(expectedStashList.size(), actualStashList.size());

        // First compare the two lists...
        for (int i = 0; i < actualStashList.size(); i++) {
            Assertions.assertEquals(expectedStashList.get(i), actualStashList.get(i));
        }
        // ... Then pop the stashed entries and clean working directory.
        for (RevCommit stash : actualStashList) {
            git.popStash(stash.getName());
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
        }
        Assertions.assertEquals(0, git.stashList().call().size());

        // Delete all sample files created in this test
        cleanRootDirectory();
    }

    @Test @Order(5)
    public void stashAndApplyChangesTest() throws GitAPIException, IOException {

        File sampleFile = createAndCommitSampleFile("sample2.txt", "initial sample text");

        // Make sure that no stashes are present
        Assertions.assertTrue(git.stashList().call().isEmpty());

        git.checkoutBranch("foo", true);

        // Make sure that checkout didn't store any stashes
        Assertions.assertTrue(git.stashList().call().isEmpty());

        FileUtils.write(sampleFile, "different sample text", charset, false);
        git.commit("Edit sample file", true);

        FileUtils.write(sampleFile, "yet more different sample text", charset, false);

        // Checkout here should shelf changes due to unresolved conflicts
        git.checkoutBranch("master", false);

        // A single stash should be present in both the stashList and StashShelf
        Assertions.assertEquals(1, git.stashList().call().size());
        Assertions.assertEquals(1, git.getStashShelf().getTrackedEntries("foo").size());

        git.checkoutBranch("foo", false);

        // Stash was tracked so it should have been automatically applied to the checked-out branch.
        // This means it should have also been removed from the stashList and StashShelf
        Assertions.assertEquals(0, git.stashList().call().size());
        Assertions.assertEquals(0, git.getStashShelf().getTrackedEntries("foo").size());

        // Delete all sample files created in this test
        cleanRootDirectory();
    }

    /**
     * Delete repository root directory and all contained files.
     * @throws IOException in case deletion is unsuccessful
     */
    @Test
    public void deleteRepository() throws IOException {

        FileUtils.deleteDirectory(path.toFile());
        Assertions.assertFalse(path.toFile().exists());
    }

    private File createSampleFile(String name, String... content) throws IOException {

        File sampleFile = path.resolve(name).toFile();
        Assertions.assertTrue(sampleFile.createNewFile());
        Assertions.assertTrue(sampleFiles.add(sampleFile));

        if (content.length != 0) {
            FileUtils.write(sampleFile, String.join(System.lineSeparator(), content), charset);
        }
        return sampleFile;
    }

    private File createAndCommitSampleFile(String name, String... content) throws GitAPIException, IOException {

        File sampleFile = createSampleFile(name, content);

        git.add(sampleFile.toPath());
        git.commit("Create " + name + " file", false);
        return sampleFile;
    }

    private void cleanRootDirectory() {

        sampleFiles.forEach(f -> f.delete());
        sampleFiles.clear();
    }
}

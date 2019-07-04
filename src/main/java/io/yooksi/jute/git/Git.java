package io.yooksi.jute.git;

import io.yooksi.jute.bash.UnixPath;
import io.yooksi.commons.define.LineSeparator;
import io.yooksi.commons.define.MethodsNotNull;
import io.yooksi.commons.logger.LibraryLogger;

import io.yooksi.commons.util.StringUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.StashApplyFailureException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.FileUtils;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.Positive;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@MethodsNotNull
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class Git extends org.eclipse.jgit.api.Git {

    private final UnixPath repoRootDirPath;
    private final StashShelf stashShelf;

    /**
     * Construct a new {@link Git} object which can interact with the specified git repository.
     * <p>
     * All command classes returned by methods of this class will always
     * interact with this git repository.
     * <p>
     * The caller is responsible for closing the repository; {@link #close()} on
     * this instance does not close the repo.
     *
     * @param repo the git repository this class is interacting with;
     *             {@code null} is not allowed.
     */
    public Git(Repository repo) {
        super(repo);
        repoRootDirPath = UnixPath.get(repo.getDirectory().getParentFile());
        stashShelf = new StashShelf(this);
    }

    public StashShelf getStashShelf() {
        return stashShelf;
    }

    /**
     * Open Git repository located in root directory.
     *
     * @return a {@link org.eclipse.jgit.api.Git} object for the git repository in root directory
     * @throws IllegalStateException if the git repository was not found
     *
     * @see org.eclipse.jgit.api.Git#open(File)
     */
    public static Git openRepository() {

        try {
            LibraryLogger.debug("Opening Git repository in root directory.");
            return new Git(open(new File(".git")).getRepository());
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to open Git repository in root directory", e);
        }
    }

    /**
     * Open Git repository located under given path.
     *
     * @param repoPath {@code Path} to the repository to open
     * @return a {@link org.eclipse.jgit.api.Git} object for the existing git repository
     *
     * @throws java.io.FileNotFoundException if the file represented by the given path does not exist.
     * @throws IOException if the repository could not be accessed to configure builder's parameters.
     *
     * @see org.eclipse.jgit.api.Git#open(File)
     */
    public static Git openRepository(Path repoPath) throws IOException {

        File repo = repoPath.toFile();
        if (!repo.exists()) {
            String log = "Unable to find Git repository under path \"%s\"";
            throw new java.io.FileNotFoundException(String.format(log, repoPath.toAbsolutePath().toString()));
        }
        else {
            LibraryLogger.debug("Opening Git repository under path " + repoPath.toAbsolutePath().toString());
            return new Git(open(repo).getRepository());
        }
    }

    /**
     * <p>Create an empty git repository or reinitialize an existing one.</p>
     * <p>
     *     This command creates an empty Git repository - basically a {@code .git} directory
     *     with subdirectories for {@code objects, refs/heads, refs/tags}, and template files.
     *     An initial {@code HEAD} file that references the {@code HEAD}
     *     of the master branch is also created.
     * </p>
     *     Running <i>git init</i> in an existing repository is safe.
     *     It will not overwrite things that are already there.
     *     The primary reason for rerunning <i>git init</i> is to pick up newly added templates
     *     (or to move the repository to another place if --separate-git-dir is given).
     * </p>
     * @param rootDirPath {@code Path} to the directory we want to initialize our repository in.
     *                    Note that path can <i>(but should not)</i> point to the repository meta
     *                    directory ({@code .git}) in which case a warning will be logged.
     *
     * @return an instance of the (re)initialized repository
     * @throws GitAPIException if an exception occurred while executing {@link InitCommand}
     *
     * @see org.eclipse.jgit.api.Git#init()
     */
    public static Git initRepository(Path rootDirPath) throws IOException, GitAPIException {

        File rootDir = rootDirPath.toFile();
        InitCommand command;

        if (!rootDir.getName().equals(".git")) {
            command = init().setDirectory(rootDir);
        }
        else {
            LibraryLogger.warn("Tried to initialize a Git repository inside a recursive directory (.git\\.git).");
            command = init().setGitDir(rootDir);
        }
        if (!rootDir.exists()) {
            FileUtils.mkdirs(rootDir);
        }
        String absPath = rootDirPath.toAbsolutePath().toString();
        LibraryLogger.debug("Initializing Git repository in directory " + absPath);
        return new Git(command.call().getRepository());
    }

    /**
     * @return {@code String} form of the commit's SHA-1, in lower case hexadecimal.
     */
    public static String getCommitSHA(RevCommit commit) {
        return commit.toObjectId().getName();
    }

    /**
     * Create a new commit containing the current contents of the index and the given log message
     * describing the changes. The new commit is a direct child of {@code HEAD}, usually the tip
     * of the current branch, and the branch is updated to point to it (unless no branch is
     * associated with the working tree, in which case {@code HEAD} is "detached" as described
     * in <a href=https://git-scm.com/docs/git-checkout>git-checkout[1]</a>)
     *
     * @param message the commit message
     * @param stageAll If set to {@code true} the Commit command automatically stages files that have
     * 	               been modified and deleted, but new files not known by the repository are not affected.
     * 	               This corresponds to the parameter {@code -a} on the command line.
     *
     * @return a reference to the resulting commit
     * @throws GitAPIException if an exception occurred while executing {@link CommitCommand}
     *
     * @see org.eclipse.jgit.api.Git#commit()
     */
    public RevCommit commit(String message, boolean stageAll) throws GitAPIException {

        LibraryLogger.debug("Committing all indexed files");
        return commit().setAll(stageAll).setMessage(message).call();
    }

    /**
     * Add a path to a {@code file/directory} whose content should be added.
     * A directory name (e.g. dir to add dir/file1 and dir/file2) can
     * also be given to add all files in the directory, recursively.
     *
     * @param path {@code Path} to the file to add
     * @return reference to the index file just added
     * @throws GitAPIException if an exception occurred while executing {@link AddCommand}
     *
     * @see org.eclipse.jgit.api.Git#add()
     * @see AddCommand#addFilepattern(String)
     */
    public DirCache add(Path path) throws GitAPIException {

        LibraryLogger.debug("Adding \"%s\" to indexed files.", path.toString());
        return add().addFilepattern(relativizePath(path).toString()).call();
    }

    /**
     * Updates files in the working tree to match the version in the index or the specified tree.
     * If no paths are given, git checkout will also update {@code HEAD} to set the specified branch
     * as the current branch. If the checkout operation fails with {@link CheckoutConflictException}
     * due to unresolved conflicts the changes will be stored to a {@link StashShelf} with tracking
     * enabled with will automatically pop the changes the next time this branch is checked out.
     *
     * @param branch name of the branch to checkout
     * @param create whether to create the branch if it does not already exist
     * @return a reference to the checked out branch
     *
     * @throws IOException This exception is thrown from {@link RefDatabase#exactRef(String)}
     *                     when the reference space under given branch cannot be accessed.
     *
     * @throws GitAPIException if an exception occurred while executing {@link CheckoutCommand)}.
     * @throws CheckoutConflictException if the checkout operation failed due to unresolved conflicts.
     * @throws CreateBranchFailedException if branch creation failed because the branch already exists,
     *                                     or an unknown reason is preventing the branch from being created.
     *
     * @see org.eclipse.jgit.api.Git#checkout()
     * @see CheckoutCommand#setCreateBranch(boolean)
     */
    public Ref checkoutBranch(String branch, boolean create) throws IOException, GitAPIException {

        String currentBranch = getRepository().getBranch();
        try {
            Ref result = checkoutBranchInternal(currentBranch, branch, create);
            stashShelf.popTrackedEntries(branch);
            return result;
        }
        /* This exception will be thrown when we cant checkout because of unresolved conflicts.
         * So we're gonna stash the changes and try to checkout the branch again
         */
        catch (CheckoutConflictException e1)
        {
            LibraryLogger.warn("Local changes to files would be overwritten by checkout. " +
                    "Going to shelf the changes and try checking out branch again");

            /* Store the changes in a shelf and enable tracking so they will be
             * automatically popped the next time we checkout this branch
             */
            shelfChanges(true);

            Ref result = checkoutBranchInternal(currentBranch, branch, create);
            stashShelf.popTrackedEntries(branch);
            return result;
        }
    }
    /**
     * Internal method used to checkout branch.
     * @see #checkoutBranch(String, boolean)
     */
    private Ref checkoutBranchInternal(String oldBranch, String newBranch, boolean create) throws GitAPIException, IOException {

        CheckoutCommand cmd = checkout().setName(newBranch);
        if (create)
        {
            if (getRepository().findRef(newBranch) == null)
            {
                LibraryLogger.debug("Creating and checking out new branch " + newBranch);
                Ref result = cmd.setCreateBranch(true).call();

                if (oldBranch.equals(getRepository().getBranch())) {
                    throw new CreateBranchFailedException(newBranch);
                }
                else return result;
            }
            else throw new CreateBranchFailedException(newBranch, "target already exists");
        }
        else LibraryLogger.debug("Checking out branch " + newBranch);
        return cmd.call();
    }

    /**
     * Retrieve last commit reference with {@link LogCommand} then parse
     * and decode the complete commit message to a string.
     *
     * @return last commit's full message
     *
     * @throws NoHeadException if no HEAD exists and no explicit starting revision was specified.
     * @throws GitAPIException if an exception occurred while executing {@link LogCommand}.
     *
     * @see org.eclipse.jgit.api.Git#log()
     * @see RevCommit#getFullMessage()
     */
    public String getLastCommitMessage() throws GitAPIException {

        java.util.Iterator<RevCommit> commits = log().call().iterator();
        return commits.hasNext() ? commits.next().getFullMessage() : "";
    }

    /**
     * Compile and return a {@code List} of all commits found on the current branch.
     * The order of list elements will be the same as the order at which they were originally compiled
     * by {@link LogCommand#all()}. This insertion sorting order should naturally correlate to values
     * provided by {@link RevCommit#commitTime} from newest to oldest. In short the newest commit will
     * be the first element, and the oldest commit will be the last element of the returned list.
     *
     * @return {@code List} of all commits found on the current branch.
     * @throws IOException if some commit references could not be accessed
     * @throws GitAPIException if an exception occurred while executing {@link LogCommand}.
     *
     * @see org.eclipse.jgit.api.Git#log()
     */
    public List<RevCommit> getAllCommits() throws IOException, GitAPIException {

        List<RevCommit> commits = new ArrayList<>();
        log().all().call().forEach(commits::add);
        return commits;
    }

    /**
     * @return a reference on the current branch that corresponds to {@code HEAD}.
     * @throws IOException if the reference space cannot be accessed.
     */
    public Ref getHead() throws IOException {
        return getRepository().findRef(Constants.HEAD);
    }

    /**
     * Rewind the current {@code HEAD} for {@code N} steps using the specified mode.
     * This operation is equivalent to executing {@code git reset HEAD~N} with {@code N}
     * being the number of commits to reset. Internally the method will retrieve a list of
     * all commits currently residing on the current branch and perform the {@code reset}
     * command with the {@code ref} set to the {@code SHA} of the {@code Nth} commit.
     *
     * @param steps number of commits (from {@code HEAD}) to rewind.
     * @param mode {@code ResetType} to use when configuring the reset command.
     * @return reference to the new {@code HEAD} of the current branch.
     *
     * @throws IOException thrown by {@link #getAllCommits()} if some commit references could not be accessed.
     * @throws GitAPIException if an exception occurred while executing {@link ResetCommand}.
     *
     * @see org.eclipse.jgit.api.Git#reset()
     */
    public Ref rewind(@Positive int steps, @Nullable ResetCommand.ResetType mode) throws IOException, GitAPIException {

        if (steps < 0) {
            String log = "Cannot reset current HEAD for %d steps. Value must be a positive number.";
            throw new IllegalArgumentException(String.format(log, steps));
        }
        List<RevCommit> commits = getAllCommits();
        if (steps >= commits.size())
        {
            String log = "Cannot reset current HEAD for %d steps. Not enough commits (%d) to accommodate the request.";
            throw new IndexOutOfBoundsException(String.format(log, steps, commits.size()));
        }
        String ref = commits.get(steps).getName();
        LibraryLogger.debug("Performing %s reset on HEAD to ref \"%s\"", mode, ref);
        return reset().setRef(ref).setMode(mode != null ? mode : ResetCommand.ResetType.MIXED).call();
    }

    /**
     * Internal method to stash local changes decoupled to delegate the responsibility
     * of retrieving the name of the current branch and the {@code IOException} that throws.
     *
     * @param branch name of the branch to include in the debug log.
     * @return reference to the stashed commit
     *
     * @throws GitAPIException if an exception occurred while executing {@link StashCreateCommand}
     * @see #stashChanges()
     */
    private RevCommit stashChanges(String branch) throws GitAPIException {

        LibraryLogger.debug("Stashing local changes on branch " + branch);
        return stashCreate().call();
    }

    /**
     * Use this method when you want to record the current state of the working directory and the index,
     * but want to go back to a clean working directory. The command saves your local modifications away
     * and reverts the working directory to match the {@code HEAD} commit.
     *
     * @return reference to the stashed commit
     *
     * @throws GitAPIException if an exception occurred while executing {@link StashCreateCommand}
     * @throws IOException when we're unable to resolve the current branch
     *
     * @see org.eclipse.jgit.api.Git#stashCreate()
     */
    public RevCommit stashChanges() throws GitAPIException, IOException {
        return stashChanges(getRepository().getBranch());
    }

    /**
     * Stash local changes and add shelf the resulting stash reference.
     *
     * @param track whether to track the created shelf entry.
     * @return reference to the stashed commit
     *
     * @throws GitAPIException if an exception occurred while executing {@link StashCreateCommand}
     * @throws IOException when we're unable to resolve current branch
     *
     * @see #stashChanges()
     * @see StashShelf#add(RevCommit, String, boolean)
     */
    public RevCommit shelfChanges(boolean track) throws IOException, GitAPIException {

        String branch = getRepository().getBranch();
        return stashShelf.add(stashChanges(branch), branch, track);
    }

    /**
     * @return a {@code List} of stashed commits in this repository.
     * @throws GitAPIException if an exception occurred while executing {@link StashListCommand}
     */
    public List<RevCommit> getStashList() throws GitAPIException {
        return new ArrayList<>(stashList().call());
    }

    /**
     * Retrieve the index number that corresponds to the position of the given stash inside the
     * stash reflog: {@code refs/stash}. For example; accessing a reflog with an index value of 0
     * ({@code stash@{0}}) would give us the most recently created stash while an index value of 1
     * ({@code stash@{1}}) would give us the stash created before it.
     *
     * @param stashSha {@code SHA-1} representation of the stash to retrieve the index for
     * @return the index number that corresponds to the position of the given stash inside
     *         the stash reflog or more technically the index position of the given stash
     *         in the {@code List} returned by {@link StashListCommand}. A negative value
     *         {@code -1} will be returned if the stash reference could not be resolved.
     *
     * @throws GitAPIException if an exception occurred while calling {@link #getStashList()}.
     */
    private int getStashIndex(String stashSha) throws GitAPIException {

        List<RevCommit> stashList = getStashList();
        for (int i = 0; i < stashList.size(); i++) {
            if (stashList.get(i).getName().equals(stashSha))
                return i;
        }
        return -1;
    }

    /**
     * Internal method to apply a stash to the working directory.
     *
     * @param stashSha {@code SHA-1} representation of the stash reference to apply.
     * @return {@code SHA-1} reference abstraction of the stash applied.
     *
     * @throws StashApplyFailureException if the given {@code SHA} does not correspond to an existing stash.
     * @throws GitAPIException if an exception occurred while executing {@link StashApplyCommand}
     *
     * @see #applyStash(String)
     */
    private ObjectId applyStashInternal(String stashSha) throws GitAPIException {

        if (!stashSha.isEmpty()) {
            if (getStashIndex(stashSha) >= 0) {
                return stashApply().setStashRef(stashSha).call();
            }
            String log = "Unable to apply stash %s, SHA does not correspond to an exiting stash..";
            throw new StashApplyFailureException(String.format(log, stashSha));
        }
        else return stashApply().call();
    }

    /**
     * Apply a stash with a reference that matches the given {@code SHA-1} to the working directory.
     * This operation is similar to {@code pop}, but does not remove the state from the stash list.
     * Unlike {@code pop, <stash>} may be any commit that looks like a commit
     * created by {@code stash push} or {@code stash create}.
     *
     * @param stashSha {@code SHA-1} representation of the stash reference to apply. This will default to
     *                 apply the latest stashed commit ({@code stash@{0}}) if {@code null} or empty.
     *
     * @throws GitAPIException if an exception occurred while executing
     *                         {@link StashApplyCommand} or {@link StashListCommand}.
     *
     * @throws IOException when we're unable to resolve current branch
     *
     * @see org.eclipse.jgit.api.Git#stashApply()
     */
    public void applyStash(String stashSha) throws GitAPIException, IOException {

        String branch = getRepository().getBranch();
        LibraryLogger.debug("Applying stashed changes on branch " + branch);

        if (stashList().call().isEmpty()) {
            LibraryLogger.warn("Unable to apply stash to working tree, stashList is empty.");
        }
        else stashShelf.remove(applyStashInternal(stashSha));
    }

    /**
     * Apply the changes in the last stashed commit to the working directory.
     *
     * @throws GitAPIException if an exception occurred while applying stash.
     * @throws IOException when we're unable to resolve current branch.
     *
     * @see #applyStash(String)
     */
    public void applyStash() throws GitAPIException, IOException {
        applyStash(StringUtils.EMPTY);
    }

    /**
     * <p>
     *     Remove a single stashed state from the stash list and apply it on top of the current working tree state,
     *     i.e., do the inverse operation of {@code git stash push}. The working directory must match the index.
     *     Applying the state can fail with conflicts; in this case, it is not removed from the stash list.
     *     You need to resolve the conflicts by hand and call {@code git stash drop} manually afterwards.
     * </p><p>
     *     This operation is equivalent to calling {@code git stash apply} followed by {@code git stash drop},
     *     which would programmatically translate to calling {@link #applyStash(String)} followed by executing
     *     a {@link StashDropCommand} with the stash ref set to the given stash {@code SHA}.
     * </p>
     * @param stashSha {@code SHA-1} representation of the stash to pop. For both apply and drop operations
     *                 this will default to the latest stashed commit ({@code stash@{0}}) if {@code null} or empty.
     *
     * @throws IOException thrown by {@link #applyStash(String)}} when the current branch is unable to be resolved.
     * @throws StashApplyFailureException if the given {@code SHA} does not correspond to an existing stash.
     * @throws GitAPIException if an exception occurred when executing {@link StashListCommand} or {@link StashDropCommand}.
     *
     * @see <a href=https://git-scm.com/docs/git-stash#Documentation/git-stash.txt-pop--index-q--quietltstashgt>
     *      Git documentation about Pop</a>
     */
    public void popStash(String stashSha) throws GitAPIException, IOException {

        int index = getStashIndex(stashSha);
        if (index >= 0) {
            LibraryLogger.debug("Popping stash@{%d}: %s", index, stashSha);
            applyStash(stashSha); stashDrop().setStashRef(index).call();
        }
        else {
            String log = "Unable to pop stash %s, SHA does not correspond to an exiting stash.";
            throw new StashApplyFailureException(String.format(log, stashSha));
        }
    }

    // ###########################################################

    public String[] diff(@Nullable TreeFilter filter) throws GitAPIException, IOException {

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            diff().setPathFilter(filter == null ? TreeFilter.ALL : filter).setOutputStream(stream).call();
            return stream.toString(Charset.defaultCharset()).split(LineSeparator.Unix);
        }
    }

    public List<DiffEntry> diff(AbstractTreeIterator from, AbstractTreeIterator to, java.io.OutputStream out,
                                @Nullable TreeFilter filter) throws GitAPIException {

            return diff().setOldTree(from).setNewTree(to)
                    .setPathFilter(filter == null ? TreeFilter.ALL : filter).setOutputStream(out).call();
    }

    // ###########################################################

    /**
     * Construct and return a path relative to repository root directory path.
     * The given path can be both {@code Unix} or {@code Windows} compatible.
     *
     * @param path {@code Path} to relativize
     * @return {@code UnixPath} that represents a relative path between repository
     *         root directory path and the given path.
     *
     * @see FileUtils#relativizeGitPath(String, String)
     */
    public UnixPath relativizePath(Path path) {
        return UnixPath.get(FileUtils.relativizeGitPath(repoRootDirPath.toString(), UnixPath.convert(path)));
    }
}

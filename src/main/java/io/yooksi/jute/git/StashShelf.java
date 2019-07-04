package io.yooksi.jute.git;

import io.yooksi.commons.define.MethodsNotNull;
import io.yooksi.commons.logger.LibraryLogger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.StashApplyFailureException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@MethodsNotNull
@SuppressWarnings("WeakerAccess")
public class StashShelf {

    private final Git git;

    private final java.util.Map<String, String> shelfEntries =
            java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<>());

    private final java.util.List<String> trackedEntries =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    StashShelf(Git git) {
        this.git = git;
    }

    /**
     * Store the given {@code RevCommit} that represents a stashed set of changes
     * in this shelf and pair it with a specified branch name. Optionally this entry
     * can be tracked which means it will be automatically popped once the specified
     * branch is checked out, in which case the entry will be removed from the shelf.
     *
     * @param stash reference to the stashed commit. Note that adding duplicate shelf entries
     *              is <b>not allowed</b> and trying to do this will throw and exception.
     *
     * @return reference to the stashed commit
     *
     * @throws UnsupportedOperationException if this shelf already contains the given stash.
     * @throws IllegalArgumentException if the given {@code RevCommit} is {@code null}
     *                                  or the branch name is an empty {@code String}.
     */
    @Contract("!null, !null, _ -> param1")
    @SuppressWarnings("ConstantConditions")
    public synchronized RevCommit add(RevCommit stash, String branch, boolean track) {

        String stashSha = stash != null ? stash.getName() : "";

        if (stash == null || branch.isEmpty()) {
            String log = "Unable to resolve stash(%s) or branch(%s).";
            throw new IllegalArgumentException(String.format(log, stash == null ? stash.getName() : "null", branch));
        }
        else if (shelfEntries.containsKey(stashSha)) {
            throw new UnsupportedOperationException("It is not allowed to override StashShelf entries.");
        }
        LibraryLogger.debug("Shelfing stash %s for branch %s.", stashSha, branch);
        shelfEntries.put(stashSha, branch);
        if (track) trackedEntries.add(stashSha);
        return stash;
    }

    /**
     * Internal method to remove the given stash {@code SHA-1} from this shelf.
     * @see #remove(ObjectId)
     */
     private synchronized void remove(String stashSha) {

        LibraryLogger.debug("Removing stash %s from shelf.", stashSha);

        if (shelfEntries.remove(stashSha) == null) {
            LibraryLogger.warn("Tried to remove unregistered stash " + stashSha);
        }
        else trackedEntries.remove(stashSha);
    }

    /**
     * Remove the given {@code RevCommit} from this shelf.
     * Note that it's safe to pass any object to this method but trying to
     * remove a stash that has not been added to this shelf will throw a warning.
     *
     * @param stash reference to the stashed commit
     */
    public synchronized void remove(ObjectId stash) {
        remove(stash.getName());
    }

    /**
     * @return {@code true} if <i>at least one</i> stash is being tracked
     *          for the given branch and {@code false} otherwise.
     */
    public synchronized boolean trackingStashForBranch(String branch) {
        return shelfEntries.containsValue(branch);
    }

    /**
     * @return a {@code List} of tracked stash entries for the given branch.
     */
    public synchronized java.util.List<String> getTrackedEntries(String branch) {

        return shelfEntries.entrySet().stream()
                .filter(e -> e.getValue().equals(branch) && trackedEntries.contains(e.getKey()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    /**
     * Execute {@link Git#popStash(String)} for each shelved entry that is being tracked for the
     * given branch. Each performed operation is equivalent to {@code git stash pop <shelved stash>}.
     *
     * @param branch name of the branch for tracked entries
     * @return {@code true} if all matched tracked entries were successfully popped.
     *
     * @throws GitAPIException if an exception occurred while executing {@link Git#popStash(String)}
     * @throws StashApplyFailureException if one of the matched entries could not be popped.
     * @throws IOException when we're unable to resolve the current branch
     */
    public synchronized boolean popTrackedEntries(String branch) throws GitAPIException, IOException {

        if (trackingStashForBranch(branch))
        {
            java.util.List<String> stashList = getTrackedEntries(branch);
            final int size = stashList.size();
            if (size > 0)
            {
                String quantifier = size > 1 ? "entries" : "entry";
                LibraryLogger.debug("Applying %d stash %s to branch %s", size, quantifier, branch);

                for (String stashSha : stashList) {
                    git.popStash(stashSha);
                }
                return !stashList.isEmpty();
            }
        }
        return false;
    }
}

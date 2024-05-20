package gitlet;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository implements Serializable {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File REPO_DIR = join(GITLET_DIR, "repository");
    public static final File COMMIT_DIR = join(GITLET_DIR, "commit");
    public static final File STAGING = join(GITLET_DIR, "staging");
    public static final File REMOVAL = join(GITLET_DIR, "remove_staging");
    public static final File HEAD = join(GITLET_DIR, "head");
    public static final File MASTER = join(GITLET_DIR, "master");
    public static final File BLOB = join(GITLET_DIR, "blob");
    public static final File BRANCHES = join(GITLET_DIR, "branches");
    private static Repository repo;
    private String currBranch;
    private String head;
    private String master;
    private Map<String, String> stageAdd; //  file(name), Blob(id)
    private Map<String, String> stageRemove; //  file(name), Blob(id)
    private ArrayList<String> trashBlob;
    private Map<String, String[]> branches;
    private ArrayList<String> cwd;
    private ArrayList<String> untracked;
    private ArrayList<String> modified;

    public static void setupPersistance() {
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdirs();
            COMMIT_DIR.mkdirs();
            STAGING.mkdirs();
            REMOVAL.mkdirs();
            BLOB.mkdirs();
            BRANCHES.mkdirs();
        }
    }

    public static void init() {

        if (REPO_DIR.exists()) {
            System.out.println("A Gilet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        } else {
            repo = new Repository();
            repo.head = Commit.init();
            String[] temp = {null, repo.head};
            repo.branches.put(repo.currBranch, temp);
            repo.saveRepo();

        }

    }
    public static void rebuild() {
        try {
            Repository temp = new Repository();
            repo = readObject(REPO_DIR, temp.getClass());
            Commit.rebuild(repo.head);
            repo.cwd = Commit.getCWD();
            for (String file: repo.cwd) {
                String bId = sha1(serialize(readContentsAsString(join(CWD, file))), file);
                if (!Commit.matchesBlob(bId)) {
                    repo.modified.add(file);
                }
            }

        } catch (IllegalArgumentException e) {
            //just calls init
        }
    }

    public static void add(String fileName) {
        if (!join(CWD, fileName).exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        } else {
            String bId = createBlob(fileName);
            String content = readContentsAsString(join(CWD, fileName)); //new content
            //check if this version of same fileName has same content, or has same
            // blob id
            if (Commit.matchesBlob(bId)) {
                if (repo.stageRemove.containsKey(fileName)) {
                    repo.stageRemove.remove(fileName);
                }
                //Commit checking
            } else if (repo.stageAdd.containsKey(fileName)) {
                repo.trashBlob.add(repo.stageAdd.get(fileName));
                repo.stageAdd.remove(fileName);
            } else {
                repo.stageAdd.put(fileName, bId);
                repo.cwd.add(fileName);
                repo.untracked.remove(fileName);
            }
            repo.saveRepo();
        }

    }

    public static void commit(String message) { //after clear make new stage
        if (repo.stageAdd.isEmpty() && repo.stageRemove.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        } else {
            repo.saveFiles();
            repo.head = Commit.commit(message, repo.stageAdd, repo.stageRemove);
            repo.stageAdd.clear();
            repo.stageRemove.clear();
            setCommitBranch();
            repo.saveRepo();
        }
    }

    public static void rm(String fileName) {
        //remove from add_stage and put into remove_stage
        //also remove from current commit
        //edit commit to remove everything from remove_stage
        if (repo.stageAdd.containsKey(fileName)) {
            repo.stageAdd.remove(fileName);
        } else { //check if is in the head commit
            try {
                String bId = createBlob(fileName);
                if (Commit.matchesBlob(bId)) {
                    repo.stageRemove.put(fileName, bId);
                    restrictedDelete(fileName);
                } else {
                    System.out.println("No reason to remove the file.");
                    System.exit(0);
                }
            } catch (IllegalArgumentException e) {
                repo.stageRemove.put(fileName, "");
            }
        }
        repo.saveRepo();
    }

    public static void log() {
        //Make function in commit called log
        //Rebuild parent
        //Call parent.printCurrentCommit();
        Commit.log();
        repo.saveRepo();
    }

    public static void globalLog() {
        Commit.gLog();
        repo.saveRepo();
    }

    public static void find(String msg) {
        Commit.find(msg);
        repo.saveRepo();
    }

    public static void status() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initiallized Gitlet directory");
        } else {
            System.out.println("=== Branches ===");
            for (String k : repo.branches.keySet()) {
                if (repo.currBranch.equals(k)) {
                    System.out.println("*" + k);
                } else {
                    System.out.println(k);
                }
            }
            System.out.println();

            //Do Staged
            System.out.println("=== Staged Files ===");
            for (String k : repo.stageAdd.keySet()) {
                System.out.println(k);
            }
            System.out.println();

            //Do Removed Files
            System.out.println("=== Removed Files ===");
            for (String k : repo.stageRemove.keySet()) {
                System.out.println(k);
            }
            System.out.println();

            System.out.println("=== Modifications Not Staged For Commit ===");
//            for (String file : Commit.getCWD()) {
//                if (!repo.stageRemove.containsKey(file) && !repo.cwd.contains(file)) {
//                    System.out.println(file + " (deleted)");
//                }
//            }
//            for (String file : repo.modified) {
//                System.out.println(file + " (modified)");
//            }
            System.out.println();


            System.out.println("=== Untracked Files ===");
            for (String str: repo.untracked) {
                System.out.println(str);
            }
            System.out.println();
            repo.saveRepo();
        }
    }

    public static void checkout(String name) {
        //Usage 3
            //Name is name of branch
        if (repo.branches.containsKey(name)) {
            if (repo.currBranch.equals(name)) {
                System.out.println("No need to checkout the current branch");
                System.exit(0);
            } else if (repo.untracked.size() > 0) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            } else {
                for (String fileName: repo.cwd) {
                    restrictedDelete(join(CWD, fileName));
                }
                repo.currBranch = name;
                repo.head = repo.branches.get(name)[1];
                Commit.rebuild(repo.head);
                ArrayList<String> lastCWD = Commit.getCWD();
                repo.cwd.clear();

            }
        } else {
            System.out.println("No such branch exists.");
        }
        repo.saveRepo();
    }

    public static void checkout(String cId, String fName) {
        //Usage 1
        if (cId.equals("head")) {
            int retVal = Commit.rebuildFile(fName, repo.head);
            Commit temp = new Commit("temp");
            Map<String, String> cMap = temp.getWorkingDirOfCommit(repo.head);
            String bId = cMap.get(fName);
            String content = readContentsAsString(join(BLOB, bId));
            if (retVal == 1) {
                System.out.println("No commit with that id exists.");
            } else if (retVal == 2) {
                System.out.println("File does not exist in that commit.");
            } else {
                writeContents(join(CWD, fName), content);
            }
        } else { //usage 2

            int retVal = Commit.rebuildFile(fName, cId);
            Commit temp = new Commit("temp");
            if (retVal == 1) {
                System.out.println("No commit with that id exists.");
            } else if (retVal == 2) {
                System.out.println("File does not exist in that commit.");
            } else {
                Map<String, String> pastMap = Commit.getWorkingDirOfCommit(cId);

                String content = readContentsAsString(join(BLOB, pastMap.get(fName)));
                writeContents(join(Repository.CWD, fName), content);
            }
        }
        repo.saveRepo();
    }

    public static void branch(String name) {
        if (repo.branches.containsKey(name)) {
            System.out.println("A branch with that name already exists.");
        } else {
            repo.branches.put(name, createCommitBranch());
        }
        repo.saveRepo();
    }

    public static void removeBranch(String name) {
        if (repo.currBranch.equals(name)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        if (!repo.branches.containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        repo.branches.remove(name);
        repo.saveRepo();
    }

    public static void reset(String id) {

    }

    public static void merge(String bName) {

    }

    public static String createBlob(String fileName) {
        String content = readContentsAsString(join(CWD, fileName));
        String id = sha1(serialize(content), fileName);
        File b = join(BLOB, id);
        writeContents(b, content);
        return id;
    }

    public static void debug() {
        System.out.println("Head id: " + repo.head);
        System.out.println("Stage_add: " + repo.stageAdd);
        System.out.println("Stage_remove: " + repo.stageRemove);
        System.out.println("Trash_Blob: " + repo.trashBlob);
        for (String k: repo.branches.keySet()) {
            System.out.println("Branch: " + k + ": " + Arrays.toString(repo.branches.get(k)));
        }
        System.out.println("cwd: " + repo.cwd);
        System.out.println("Untracked: " + repo.untracked);

        Commit.debugCommit();
        repo.saveRepo();
    }

    private static String[] createCommitBranch() {
        String[] arr = {repo.head, repo.head};
        return arr;
    }

    private static void setCommitBranch() {
        String[] latest = repo.branches.get(repo.currBranch);
        latest[1] = repo.head;
    }

    private void saveFiles() {
        for (String fName: stageAdd.keySet()) {
            cwd.add(fName);
        }
    }

    private void saveRepo() {
        try {
            REPO_DIR.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeObject(REPO_DIR, this);
    }

    private Repository() {
        currBranch = "master";
        stageAdd = new HashMap<String, String>(); //  file(name), Blob(id)
        stageRemove = new HashMap<String, String>(); //  file(name), Blob(id)
        trashBlob = new ArrayList<String>();
        branches = new HashMap<String, String[]>();
        cwd = new ArrayList<String>();
        untracked = new ArrayList<String>(plainFilenamesIn(CWD));
        modified = new ArrayList<String>();
    }



}

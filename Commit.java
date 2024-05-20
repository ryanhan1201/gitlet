package gitlet;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static gitlet.Utils.*;


/** Represents a gitlet commit object.
 *
 *  Takes care of everything about commits
 *
 * @author Ryan Han
 */
public class Commit implements Serializable {
    private static Commit head;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");


    private Map<String, String> map; //blob Id, blob content
    private String message;
    private String id;
    private String parent;
    private Date date;

    public static String head() {
        if (head == null) {
            init();
        }
        return head.id;
    }

    public static String init() {
        head = new Commit("initial commit");
        head.date = new Date(0);
        head.saveCommit();
        return head.id;
    }

    public static String commit(String msg, Map<String, String> add, Map<String, String> remove) {
        Commit newHead = new Commit(msg);
        newHead.setSnaps(add, remove);
        newHead.parent = head.id;
        head = newHead;
        newHead.updateCommit();
        return head.id;
    }
    public static void rebuild(String id) {
        Commit temp = new Commit("temp");
        Commit c = readObject(join(Repository.COMMIT_DIR, id), temp.getClass());
        head = c;
    }

    public static void log() { //Start from Head
        Commit c = head;
        Commit temp = new Commit("temp");

        while (c.parent != null) {
            //printing out log
            System.out.println("===");
            System.out.println("commit " + c.id);
            System.out.println("Date: " + dateFormat.format(c.date));
            System.out.println(c.message);
            System.out.println();
            //moving values
            c = readObject(join(Repository.COMMIT_DIR, c.parent), temp.getClass());

        }
        System.out.println("===");
        System.out.println("commit " + c.id);
        System.out.println("Date: " + dateFormat.format(c.date));
        System.out.println(c.message);
        System.out.println();

    }

    public static void gLog() {
        Commit temp = new Commit("temp");
        List<String> f = plainFilenamesIn(Repository.COMMIT_DIR);
        for (int i = 0; i < f.size(); i++) {
            Commit c = readObject(join(Repository.COMMIT_DIR, f.get(i)), temp.getClass());
            System.out.println("===");
            System.out.println("commit " + c.id);
            System.out.println("Date: " + dateFormat.format(c.date));
            System.out.println(c.message);
            System.out.println();
        }
    }

    public static void find(String msg) {
        boolean found = false;
        Commit temp = new Commit("temp");
        List<String> f = plainFilenamesIn(Repository.COMMIT_DIR);
        for (int i = 0; i < f.size(); i++) {
            Commit c = readObject(join(Repository.COMMIT_DIR, f.get(i)), temp.getClass());
            if (c.message.equals(msg)) {
                System.out.println(c.id);
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }


    public static boolean matchesBlob(String bId) {
        if (head.map == null) {
            return false;
        }
        boolean foundMatch = false;
        for (String fileName: head.map.keySet()) {
            if (head.map.get(fileName).equals(bId)) {
                foundMatch = true;
            }
        }
        return foundMatch;
    }

    public static ArrayList<String> getCWD() {
        ArrayList<String> cwd = new ArrayList<String>();
        if (head.map == null) {
            return cwd;
        }
        for (String fName: head.map.keySet()) {
            cwd.add(fName);
        }
        return cwd;
    }


    public Commit(String m) {
        this.message = m;
        this.date = new Date();
    }

    public void setSnaps(Map<String, String> add, Map<String, String> remove) {
        map = add;
        if (!remove.isEmpty()) {
            for (String key: remove.keySet()) {
                map.remove(key);
            }
        }

    }

    public static void saveHead() {
        head.saveCommit();
    }


//return 1 when commit id doesnt exist
    //Retun 2 when the file doesnt exist
    public static int rebuildFile(String fileName, String commitId) {
        try {
            Commit c = readObject(join(Repository.COMMIT_DIR, commitId), head.getClass());
            if (c.map.get(fileName) == null) {
                return 2;
            }
            c = readObject(join(Repository.COMMIT_DIR, commitId), head.getClass());
        } catch (IllegalArgumentException e) {
            return 1;
        }
        return 0;
    }

    public void saveCommit() {

        this.id = sha1(serialize(this));
        File commit = join(Repository.COMMIT_DIR, id);
        try {
            commit.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        writeObject(commit, this);
    }

    public static Map<String, String> getWorkingDirOfCommit(String id) {
        Commit temp = new Commit("temp");
        Commit c = readObject(join(Repository.COMMIT_DIR, id), temp.getClass());
        return c.map;
    }

    public static void debugCommit() {
        System.out.println("Commit: ");
        System.out.println("\tParent: " + head.parent);
        System.out.println("\tmessage: " + head.message);
        System.out.println("\tDate: " + head.date);
        System.out.println("\tMap: " + head.map);
        for (String k: head.map.keySet()) {
            System.out.println(k);
        }

    }

    public void deleteCWD() {
        ArrayList<String> currCwd = getCWD();
        for (String fName: currCwd) {
            restrictedDelete(fName);
        }
    }

    public void rebuildCWD() {
        for (String fName: head.map.keySet()) {
            Repository.createBlob(head.map.get(fName));
        }
    }

//put everything together
    public void updateCommit() {
        saveCommit();
    }
}

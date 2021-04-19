package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class commitPointers {

    public static HashMap<String, String> branches = new HashMap<>();

    public static String[] Head = new String[2];

    public static ArrayList<String> commitIDList = new ArrayList<>();

    static final File CWD = new File(".");
    static final File GITLET_FOLDER = new File(CWD,".gitlet");

    //TODO: could you just make one file? and create a constructor? Similar to blob and commit (would have to make it an obj?)
    static final File BRANCH_FILE = new File(GITLET_FOLDER, "branches");
    static final File HEAD_FILE = new File(GITLET_FOLDER, "head");
    static final File COMMITLIST_FILE = new File(GITLET_FOLDER, "commitlist");

    //TODO: for status, add a new commit arraylist? or put all maps/lists into one file here
    public static void updateBranches(String branchName, String commitID) throws IOException {
        //initializes the branches if the BRANCH_file is not yet created
        if (!(BRANCH_FILE.exists())){
            BRANCH_FILE.createNewFile();
            branches.put(branchName, commitID);
        }
        else{
            branches = gitlet.Utils.readObject(BRANCH_FILE, HashMap.class);
            branches.replace(branchName, commitID);
        }
        saveBranches();
    }

    public static void updateHead(String branchName, String commitID) throws IOException {
        if (!(HEAD_FILE.exists())){
            HEAD_FILE.createNewFile();
        }

        Head[0] = branchName;
        Head[1] = commitID;
        saveHead();
    }

    public static void updateCommitIDList(String ID) throws IOException {
        if(!COMMITLIST_FILE.exists()){
            COMMITLIST_FILE.createNewFile();
        }
        else{
            commitIDList = commitPointers.readCommitIDList();
        }
        commitIDList.add(ID);
        saveCommitIDList();
    }

    //TODO: update so doesn't return anything
    public static String[] readHeadCommit(){
        Head = Utils.readObject(commitPointers.HEAD_FILE, String[].class);
        return Head;
    }

    public static HashMap readBranches(){
        branches = Utils.readObject(commitPointers.BRANCH_FILE, HashMap.class);
        return branches;
    }

    public static ArrayList readCommitIDList(){ return Utils.readObject(commitPointers.COMMITLIST_FILE, ArrayList.class); }

    public static void saveBranches(){
        Utils.writeObject(commitPointers.BRANCH_FILE, branches);
    }

    public static void saveHead(){ Utils.writeObject(commitPointers.HEAD_FILE, Head); }

    public static void saveCommitIDList(){
        Utils.writeObject(COMMITLIST_FILE, commitIDList);
    }

}
package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class Commit implements Serializable {

    static final File CWD = new File(".");
    static final File GITLET_FOLDER = new File(CWD,".gitlet");
    static final File COMMIT_FOLDER = new File(GITLET_FOLDER, "commit");

    public static boolean initial = false;
    public String message;
    public ArrayList<String> parentID = new ArrayList<>();
    public String commitID;
    public LocalDateTime timeStamp;
    public HashMap<String, String> fileBlobs = new HashMap<>();

    //TODO: mimic look of other functions
    public Commit(){
        COMMIT_FOLDER.mkdir();
        initial = true;
        timeStamp = LocalDateTime.parse("1970-01-01T00:00:00");
        message = "initial commit";
        parentID.add(null);
    }

    //TODO: make parents an array
    public Commit(String msg, ArrayList<String> parent, HashMap blobs){
        message = msg;
        timeStamp = java.time.LocalDateTime.now();
        parentID = parent;
        fileBlobs = blobs;
    }

    //https://stackoverflow.com/questions/17675804/remove-multiple-keys-from-map-in-efficient-way
    //TODO: could do with streams? <-- explore
    //Updates commit blobs: when a new commit is being created compares blobs in last commit with those in staging area.
    public void updateCommitBlobs(Map stagingArea, Map stagingRemoveArea){
        Set stageAddKeys = stagingArea.keySet();
        Set stageRemoveKeys = stagingRemoveArea.keySet();
        fileBlobs.keySet().removeAll(stageAddKeys);
        fileBlobs.keySet().removeAll(stageRemoveKeys);
        fileBlobs.putAll(stagingArea);
    }


    public void saveCommit(String commitID) throws IOException {
        this.commitID = commitID;
        File commitFile = Utils.join(COMMIT_FOLDER, commitID + ".txt");
        commitFile.createNewFile();
        Utils.writeObject(commitFile, this);

        commitPointers.updateCommitIDList(commitID);
    }
}

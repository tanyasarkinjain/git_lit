
package gitlet;

import java.io.*;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */
public class Main {

    static final File CWD = new File(".");
    static final File GITLET_FOLDER = new File(CWD,".gitlet");
    static final File STAGING_FILE = new File(GITLET_FOLDER,"staging");
    static final File STAGING_REMOVE_FILE = new File(GITLET_FOLDER,"stagingRemove");

    static HashMap<String, String> stagingArea = new HashMap<>();
    static HashMap<String, String> stagingRemoveArea = new HashMap<>();

    //TODO: format with zone
    static DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss YYY");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {

        if (args.length == 0) {
            exitWithError("Please enter a command.");
        }

        //TODO: error message for not initialized + opearnds
        if (!GITLET_FOLDER.exists() && !args[0].equals("init")) {
            exitWithError("Not in an initialized gitlet directory.");
        }
        switch (args[0]) {
            case "init":

                if ((GITLET_FOLDER.exists())) {
                    exitWithError("A Gitlet version-control system already exists in the current directory.");
                }
                init();
                break;

            case "add":
                //TODO: how to check "format" of operands?
                if (args.length != 2) {
                    exitWithError("Incorrect operands.");
                }

                add(args[1]);
                break;

            case "rm":
                if (args.length != 2) {
                    exitWithError("Incorrect operands.");
                }
                remove(args[1]);
                break;

            case "commit":
                if (args.length != 2 || args[1].equals("")) {
                    exitWithError("Please enter a commit message.");
                }

                commit(args[1]);
                break;
            case "log":
                if (args.length != 1) {
                    exitWithError("Incorrect operands.");
                }
                log();
                break;

            //fixed error 30.5?
            case "checkout":
                if (args.length == 3){
                    if (args[1].equals("--")) {
                        checkoutFile(args[2]);
                    }
                    else {
                        exitWithError("Incorrect operands.");
                    }
                }
                else if (args.length == 2){
                    checkoutBranch(args[1]);
                }
                else if (args.length == 4) {
                    if (args[2].equals("--")) {
                        checkoutCommit(args[1], args[3]);
                    }
                    else{
                        exitWithError("Incorrect operands.");
                    }
                }
                else{
                    exitWithError("Incorrect operands.");
                }
                break;

            case "branch":
                if (args.length != 2) {
                    exitWithError("Incorrect operands.");
                }
                branch(args[1]);
                break;

            case "rm-branch":
                if (args.length != 2) {
                    exitWithError("Incorrect operands.");
                }
                removeBranch(args[1]);
                break;

            case "global-log":
                if (args.length != 1) {
                    exitWithError("Incorrect operands.");
                }
                globalLog();
                break;

            case "find":
                if (args.length != 2) {
                    exitWithError("Incorrect operands.");
                }
                find(args[1]);
                break;

            case "reset":
                if (args.length != 2) {
                    exitWithError("Incorrect operands.");
                }
                reset(args[1]);
                break;

            case "status":
                if (args.length != 1) {
                    exitWithError("Incorrect operands.");
                }
                status();
                break;

            case "merge":
                if (args.length != 2) {
                    exitWithError("Incorrect operands.");
                }
                merge(args[1]);
                break;

            default:
                exitWithError("No command with that name exists.");
        }
        return;
    }

    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }

    public static void init() throws IOException {
        GITLET_FOLDER.mkdir();
        STAGING_FILE.createNewFile();
        Utils.writeObject(STAGING_FILE, stagingArea);
        STAGING_REMOVE_FILE.createNewFile();
        Utils.writeObject(STAGING_REMOVE_FILE, stagingRemoveArea);

        Commit initCommit = new Commit();
        String initCommitId = Utils.sha1(Utils.serialize(initCommit));

        commitPointers.updateBranches("master",initCommitId);
        commitPointers.updateHead("master", initCommitId);

        initCommit.saveCommit(initCommitId);
    }

    //Runtime satisfied
    public static void add(String fileName) throws IOException {

        //checks if passed in file exists in the working directory
        // shMap.class);

        File addedFile = Utils.join(CWD, fileName);

        if (!addedFile.exists()){
            exitWithError("File does not exist.");
        }

        stagingRemoveArea = Utils.readObject(STAGING_REMOVE_FILE, HashMap.class);
        if (stagingRemoveArea.containsKey(fileName)) {
            stagingRemoveArea.remove(fileName);
            Utils.writeObject(STAGING_REMOVE_FILE, stagingRemoveArea);
        }

        //creates a blob with file contents and name. Creates an ID for blob.
        Blob blob = new Blob(fileName, addedFile);
        String newBlobId = Utils.sha1(Utils.serialize(blob));

        //Gets current head commit Object + name
        String currName = commitPointers.readHeadCommit()[1];
        File cFile = Utils.join(Commit.COMMIT_FOLDER, currName + ".txt");
        Commit currCommit = Utils.readObject(cFile, Commit.class);

        //checks if blobID of blob to be stages = a blob Id in the last commit's fileBlobs.
        //if so: exits.
        //TODO: If the current working version of the file is identical to the version in the current commit, do not stage it to be added, and remove it from the staging area if it is already there

        if (currCommit.fileBlobs.size() > 0 && currCommit.fileBlobs.containsKey(fileName) && currCommit.fileBlobs.get(fileName).equals(newBlobId)){
            exitWithError("");
        }

        //Saves Blob to a file in blob folder.
        blob.saveBlob(newBlobId);

        //reads staging area and adds fileName + blob to staging area.

        stagingArea = Utils.readObject(STAGING_FILE, HashMap.class);
        stagingArea.put(fileName, newBlobId);
        Utils.writeObject(STAGING_FILE, stagingArea);
    }

    //TODO: remove
    public static void remove(String fileName) throws IOException {
        stagingArea = Utils.readObject(STAGING_FILE, HashMap.class);
        String currCommitID = commitPointers.readHeadCommit()[1];
        File cFile = Utils.join(Commit.COMMIT_FOLDER, currCommitID + ".txt");
        Commit curr = Utils.readObject(cFile, Commit.class);

        //TODO: redo if
        if(stagingArea.containsKey(fileName) || curr.fileBlobs.containsKey(fileName)) {
            if (stagingArea.containsKey(fileName)) {
                stagingArea.remove(fileName);
                Utils.writeObject(STAGING_FILE, stagingArea);
            }

            if (curr.fileBlobs.containsKey(fileName)) {
                stagingRemoveArea = Utils.readObject(STAGING_REMOVE_FILE, HashMap.class);
                stagingRemoveArea.put(fileName, curr.fileBlobs.get(fileName));
                Utils.writeObject(STAGING_REMOVE_FILE, stagingRemoveArea);

                if (Utils.join(CWD, fileName).exists()) {
                    Utils.join(CWD, fileName).delete();
                }
            }
        }

        else {
            exitWithError("No reason to remove the file.");
        }
    }

    //TODO: handle failure case: if no files staged abort
    //TODO: handle commits to different branches and organizing parents (also update head for CORRECT branch)
    public static void commit(String message) throws IOException {
        stagingArea = Utils.readObject(STAGING_FILE, HashMap.class);
        stagingRemoveArea = Utils.readObject(STAGING_REMOVE_FILE, HashMap.class);

        if (stagingArea.size() == 0 && stagingRemoveArea.size() == 0){
            exitWithError("No changes added to the commit.");
        }

        //Gets current head commit Object + name
        String currName = commitPointers.readHeadCommit()[1];
        File cFile = Utils.join(Commit.COMMIT_FOLDER, currName + ".txt");
        Commit curr = Utils.readObject(cFile, Commit.class);

        //creates a list of parent ids for the new commit object
        ArrayList<String> parentsIDs = new ArrayList<String>(1);
        parentsIDs.add(0, currName);

        //creates a new commit with the parent IDs, and the bobs contained in headCommit (curr)
        Commit newCommit = new Commit(message, parentsIDs, curr.fileBlobs);

        //Staging area work
        //TODO: move line upwards so you can check if staging area is empty before anything else
        newCommit.updateCommitBlobs(stagingArea, stagingRemoveArea);

        //makes a new commitID, and saves new commit with that ID
        String newCommitId = Utils.sha1(Utils.serialize(newCommit));
        newCommit.saveCommit(newCommitId);

        stagingArea.clear();
        stagingRemoveArea.clear();
        Utils.writeObject(STAGING_FILE, stagingArea);
        Utils.writeObject(STAGING_REMOVE_FILE, stagingRemoveArea);

        //TODO: figure out how to send in the correct branch to head
        commitPointers.updateBranches(commitPointers.readHeadCommit()[0], newCommitId);
        commitPointers.updateHead(commitPointers.readHeadCommit()[0], newCommitId);
    }

    //TODO: update checkout branch so files not in a branch are deleted? or removed??
    //need to add error case for if given branch is the current branch
    public static void checkoutBranch(String arg) throws IOException {
        File currFile = Utils.join(Commit.COMMIT_FOLDER, commitPointers.readHeadCommit()[1] + ".txt");
        Commit currCommit = Utils.readObject(currFile, Commit.class);

        if(!commitPointers.readBranches().containsKey(arg)){
            exitWithError("No such branch exists.");
        }
        if(commitPointers.readHeadCommit()[0].equals(arg)) {
            exitWithError("No need to checkout the current branch.");
        }
        File cFile = Utils.join(Commit.COMMIT_FOLDER, commitPointers.branches.get(arg) + ".txt");
        Commit checkoutCommit = Utils.readObject(cFile, Commit.class);

        for (String cwdfileName : CWD.list()) {
            if (!cwdfileName.equals(".gitlet")) {
                File cwFile = Utils.join(CWD, cwdfileName);
                if ((cwFile.exists() && !currCommit.fileBlobs.containsKey(cwdfileName) && checkoutCommit.fileBlobs.containsKey(cwdfileName))) {
                    exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            }
        }

        for (String cwdfileName : CWD.list()) {
            if (!cwdfileName.equals(".gitlet")) {
                if (!checkoutCommit.fileBlobs.containsKey(cwdfileName) && currCommit.fileBlobs.containsKey(cwdfileName)) {
                    Utils.join(CWD, cwdfileName).delete();
                }
            }
        }

        commitPointers.updateHead(arg, commitPointers.branches.get(arg));

        for (HashMap.Entry<String, String> fileBlob : checkoutCommit.fileBlobs.entrySet()) {
            checkoutFile(fileBlob.getKey());
        }
    }

    public static void checkoutFile(String fileName) throws IOException {
        String currCommitID = commitPointers.readHeadCommit()[1];
        File cFile = Utils.join(Commit.COMMIT_FOLDER, currCommitID + ".txt");
        Commit curr = Utils.readObject(cFile, Commit.class);

        if (!curr.fileBlobs.containsKey(fileName)) {
            exitWithError("File does not exist in that commit.");
        }
        File wFile = Utils.join(CWD, fileName);
        if (!wFile.exists()){
            wFile.createNewFile();
        }
        File bFile = Utils.join(Blob.BLOB_FOLDER, curr.fileBlobs.get(fileName) + ".txt");
        Blob blob = Utils.readObject(bFile, Blob.class);
        Utils.writeContents(wFile, blob.contents);
    }

    //TODO: FIX
    public static void checkoutCommit(String commitID, String fileName) throws IOException {

        if (commitID.length() < 40) {
            ArrayList<String> commitIDList = commitPointers.readCommitIDList();
            for (String commit: commitIDList) {
                if (commit.substring(0, 7).equals(commitID.substring(0, 7))) {
                    commitID = commit;
                    break;
                }
            }
        }

        File cFile = Utils.join(Commit.COMMIT_FOLDER, commitID + ".txt");

        if (!cFile.exists()) {
            exitWithError("No commit with that id exists.");
        }

        //TODO: FIXED?!!!
        Commit curr = Utils.readObject(cFile, Commit.class);
        if (!curr.fileBlobs.containsKey(fileName)) {
            exitWithError("File does not exist in that commit.");
        }
        File wFile = Utils.join(CWD, fileName);
        if (!wFile.exists()){
            wFile.createNewFile();
        }
        File bFile = Utils.join(Blob.BLOB_FOLDER, curr.fileBlobs.get(fileName) + ".txt");
        Blob blob = Utils.readObject(bFile, Blob.class);
        Utils.writeContents(wFile, blob.contents);
    }

    //TODO: do we need to handle case where branch is called before initialization? Could also put in branch class
    public static void branch(String arg){
        commitPointers.branches = commitPointers.readBranches();
        if (commitPointers.branches.containsKey(arg)){
            exitWithError("A branch with that name already exists.");
        }

        String headID = commitPointers.readHeadCommit()[1];
        commitPointers.branches.put(arg, headID);
        commitPointers.saveBranches();
    }

    public static void removeBranch(String arg){
        if (commitPointers.readHeadCommit()[0].equals(arg)){
            exitWithError("Cannot remove the current branch.");
        }
        else if (!commitPointers.readBranches().containsKey(arg)){
            exitWithError("A branch with that name does not exist.");
        }
        else{
            commitPointers.branches.remove(arg);
            commitPointers.saveBranches();
        }
    }

    //TODO: take into account multiple branches
    public static void log() {
        String branch = commitPointers.readHeadCommit()[0];
        String currName = commitPointers.readHeadCommit()[1];
        File cFile = Utils.join(Commit.COMMIT_FOLDER, currName + ".txt");
        Commit curr = Utils.readObject(cFile, Commit.class);

        while (currName != null) {
            System.out.println("===");
            System.out.println("commit " + curr.commitID);
            if (curr.parentID.size() == 2) {
                System.out.println("Merge: " + curr.parentID.get(0).substring(0, 7) + " " + curr.parentID.get(1).substring(0, 7));
            }
            System.out.println("Date: " + curr.timeStamp.format(myFormatObj) + " -0800");
            System.out.println(curr.message);
            System.out.println();

            currName = curr.parentID.get(0);

            if (currName != null) {
                File newcFile = Utils.join(Commit.COMMIT_FOLDER, currName + ".txt");
                curr = Utils.readObject(newcFile, Commit.class);
            }
        }
    }

    public static void globalLog(){

        ArrayList<String> commitIDList = commitPointers.readCommitIDList();

        for (String currName : commitIDList){
            File newcFile = Utils.join(Commit.COMMIT_FOLDER, currName+ ".txt");
            Commit curr = Utils.readObject(newcFile, Commit.class);

            System.out.println("===");
            System.out.println("commit " + curr.commitID);
            if (curr.parentID.size() == 2) {
                System.out.print("Merge: " + curr.parentID.get(0).substring(0, 7) + curr.parentID.get(1).substring(0, 7));
            }
            System.out.println("Date: " + curr.timeStamp.format(myFormatObj) + " -0800");
            System.out.println(curr.message);
            System.out.println();
        }
    }

    public static void find(String message) {
        ArrayList<String> commitIDList = commitPointers.readCommitIDList();

        int found = 0;
        for (String currName : commitIDList) {
            File newcFile = Utils.join(Commit.COMMIT_FOLDER, currName+ ".txt");
            Commit curr = Utils.readObject(newcFile, Commit.class);
            if (curr.message.equals(message)){
                System.out.println(currName);
                found += 1;
            }
        }

        if (found == 0){
            exitWithError("Found no commit with that message.");
        }
    }

    public static void status() {

        //why is staging empty
        Set branchSet = commitPointers.readBranches().keySet();
        ArrayList<String> sortedBranches = (ArrayList) branchSet.stream().sorted().collect(Collectors.toList());
        String currBranch = commitPointers.readHeadCommit()[0];
        System.out.println("=== Branches ===");
        for (String branch: sortedBranches) {
            if (currBranch.equals(branch)) {
                System.out.println("*" + currBranch);
            } else {
                System.out.println(branch);
            }
        }
        stagingArea = Utils.readObject(STAGING_FILE, HashMap.class);

        ArrayList<String> sortedStaged = (ArrayList) stagingArea.keySet().stream().sorted().collect(Collectors.toList());
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String staged : sortedStaged) {
            System.out.println(staged);
        }

        System.out.println();
        System.out.println("=== Removed Files ===");
        stagingRemoveArea = Utils.readObject(STAGING_REMOVE_FILE, HashMap.class);
        ArrayList<String> sortedRemoved = (ArrayList) stagingRemoveArea.keySet().stream().sorted().collect(Collectors.toList());
        for (String removed : sortedRemoved) {
            System.out.println(removed);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");

        Commit Head = Utils.readObject(Utils.join(Commit.COMMIT_FOLDER, commitPointers.readHeadCommit()[1] + ".txt"), Commit.class);
        //File blobs contains a SHAID + file name

        HashMap<String, String> modUntracked = new HashMap<>();

        //iterates through head Blobs
        for (HashMap.Entry<String, String> headfileBlob : Head.fileBlobs.entrySet()) {

                File cwdBFile = Utils.join(CWD, headfileBlob.getKey());

                if (!cwdBFile.exists() && !stagingRemoveArea.containsKey(headfileBlob.getKey())) {
                    modUntracked.put(headfileBlob.getKey(), "(deleted)");
                }
                else if (cwdBFile.exists()){

                    Blob tempBlob = new Blob(headfileBlob.getKey(), cwdBFile);
                    String tempBlobId = Utils.sha1(Utils.serialize(tempBlob));


                    if(!tempBlobId.equals(headfileBlob.getValue())) {
                        //if not in staging area
                        if (!stagingArea.containsKey(headfileBlob.getKey())) {
                            modUntracked.put(headfileBlob.getKey(), "(modified)");
                        }
                        //TODO: IS THIS PART NECCESARY?
                        else if (stagingArea.containsKey(headfileBlob.getKey()) && !stagingArea.get(headfileBlob.getKey()).equals(headfileBlob.getValue())) {
                            modUntracked.put(headfileBlob.getKey(), "(modified)");
                        }
                    }
                }
        }

        ArrayList<String> sortedModNames = (ArrayList) modUntracked.keySet().stream().sorted().collect(Collectors.toList());
        for (String modFileName : sortedModNames){
            System.out.println(modFileName + " " + modUntracked.get(modFileName));
        }
        System.out.println();



        ArrayList<String> untracked = new ArrayList<>();
        System.out.println("=== Untracked Files ===");
        for (String cwdfile : CWD.list()) {
            if(!cwdfile.equals(".gitlet")) {
                File currfile = Utils.join(CWD, cwdfile);
                if (currfile.exists() && !Head.fileBlobs.containsKey(cwdfile)) {
                    untracked.add(cwdfile);
                }
            }
        }

        untracked = (ArrayList) untracked.stream().sorted().collect(Collectors.toList());

        for (String untrackedName : untracked){
            System.out.println(untrackedName);
        }
    }

    public static void reset(String CommitID) throws IOException {
        String headName = commitPointers.readHeadCommit()[1];
        File hFile = Utils.join(Commit.COMMIT_FOLDER, headName + ".txt");
        Commit headCommit = Utils.readObject(hFile, Commit.class);

        if (CommitID.length() <40) {
            ArrayList<String> commitIDList = commitPointers.readCommitIDList();
            for (String commit: commitIDList) {
                if (commit.substring(0, 7).equals(CommitID.substring(0, 7))) {
                    CommitID = commit;
                    break;
                }
            }
        }
        File cFile = Utils.join(Commit.COMMIT_FOLDER, CommitID + ".txt");

        if (!cFile.exists()) {
            exitWithError("No commit with that id exists.");
        }
        Commit  givenCommit = Utils.readObject(cFile, Commit.class);

        for (String cwdfile : CWD.list()) {
            if(!cwdfile.equals(".gitlet")) {
                File currfile = Utils.join(CWD, cwdfile);
                if (currfile.exists() && !headCommit.fileBlobs.containsKey(cwdfile) && givenCommit.fileBlobs.containsKey(cwdfile)) {
                    exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            }
        }

        //TODO: does this remove tracked files not in commit?
        for (HashMap.Entry<String, String> fileBlob : givenCommit.fileBlobs.entrySet())  {
            checkoutCommit(CommitID, fileBlob.getKey());
        }


        for (Map.Entry<String, String> fileBlob : headCommit.fileBlobs.entrySet()){
            if(!givenCommit.fileBlobs.containsKey(fileBlob.getKey())){
                File cwdBFile = Utils.join(CWD, fileBlob.getKey());
                if (cwdBFile.exists()){
                    cwdBFile.delete();
                }

            }
        }

        //TODO: THE STAGING AREA IS CLEARED not removed!!!!
        stagingArea = Utils.readObject(STAGING_FILE, HashMap.class);
        stagingArea.clear();
        Utils.writeObject(STAGING_FILE, stagingArea);

        commitPointers.updateHead(commitPointers.readHeadCommit()[0], CommitID);
        commitPointers.readBranches().replace(commitPointers.readHeadCommit()[0], CommitID);
        commitPointers.saveBranches();

    }

    //Checkout removing files:
    public static void merge(String branchName) throws IOException {

        //gets commit object named curr
        String currID = commitPointers.readHeadCommit()[1];
        File cFile = Utils.join(Commit.COMMIT_FOLDER, currID + ".txt");
        Commit curr = Utils.readObject(cFile, Commit.class);

        boolean conflict = false;

        //reads staging area:
        stagingArea = Utils.readObject(STAGING_FILE, HashMap.class);
        stagingRemoveArea = Utils.readObject(STAGING_REMOVE_FILE, HashMap.class);

        if (commitPointers.readHeadCommit()[0].equals(branchName)) {
            exitWithError("Cannot merge a branch with itself.");
        }

        if (!stagingArea.isEmpty() || !stagingRemoveArea.isEmpty()) {
            exitWithError("You have uncommitted changes.");
        }

        //gets list of branches
        commitPointers.branches = commitPointers.readBranches();
        if(!commitPointers.branches.containsKey(branchName)){
            exitWithError("A branch with that name does not exist.");
        }

        //gets ID of given branch
        String branchCommitID = commitPointers.branches.get(branchName);

        File bFile = Utils.join(Commit.COMMIT_FOLDER, branchCommitID + ".txt");
        Commit currBranch = Utils.readObject(bFile, Commit.class);

        for (String cwdfile : CWD.list()) {
            if(!cwdfile.equals(".gitlet")) {
                File currfile = Utils.join(CWD, cwdfile);
                if (currfile.exists() && !curr.fileBlobs.containsKey(cwdfile) && currBranch.fileBlobs.containsKey(cwdfile)) {
                    exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            }
        }


        //TODO: fixed

        //find ID of splitPoint
        //TODO: FIX
        String splitPointName = splitPointFinder(branchName, currID);

        if (splitPointName.equals(branchCommitID)) {
            exitWithError("Given branch is an ancestor of the current branch.");
        }
        if (splitPointName.equals(currID)) {
            checkoutBranch(branchName);
            exitWithError("Current branch fast-forwarded.");
        }

        File sFile = Utils.join(Commit.COMMIT_FOLDER, splitPointName + ".txt");
        Commit splitCommit = Utils.readObject(sFile, Commit.class);


        //cases for comparing the head
        //ITERATES THROUGH CURRENT
        for (HashMap.Entry<String, String> hfileBlob : curr.fileBlobs.entrySet()) {
            //if there is a file in head not in split or branch add it.
            String hfileName = hfileBlob.getKey();
            String hfileID = hfileBlob.getValue();
            //7: Any files present at the split point, unmodified in the current branch, and absent in the given branch should be removed (and untracked).
            if(!currBranch.fileBlobs.containsKey(hfileName) && (splitCommit.fileBlobs.containsKey(hfileName)
                    && splitCommit.fileBlobs.get(hfileName).equals(hfileID))){
                remove(hfileName);
            }

            //case where given branch doesn't have file, and split des
            if(!currBranch.fileBlobs.containsKey(hfileName) && (splitCommit.fileBlobs.containsKey(hfileName)
                    && !splitCommit.fileBlobs.get(hfileName).equals(hfileID))){
                conflict = true;
                conflict(hfileID, null, hfileName);
            }
        }

        for (HashMap.Entry<String, String> bfileBlob : currBranch.fileBlobs.entrySet()) {
            String bfileName = bfileBlob.getKey();
            String bfileID = bfileBlob.getValue();

            //1: Any files that were not present at the split point and are present only in the given branch should be checked out and staged
            if (!curr.fileBlobs.containsKey(bfileName) && !splitCommit.fileBlobs.containsKey(bfileName)){
                checkoutCommit(branchCommitID, bfileName);
                stagingArea.put(bfileName, bfileID);

            }
            //6: Any files that have been modified in the given branch since the split point, but not modified in the current branch since the split point should be changed to their versions in the given branch (checked out from the commit at the front of the given branch)
            else if(curr.fileBlobs.containsKey(bfileName) && !curr.fileBlobs.get(bfileName).equals(bfileID)
                    && splitCommit.fileBlobs.containsKey(bfileName) && splitCommit.fileBlobs.get(bfileName).equals(curr.fileBlobs.get(bfileName))){
                checkoutCommit(branchCommitID, bfileName);
                stagingArea.put(bfileName, bfileID);
            }

            //case where curr doesn't have file
            if(!curr.fileBlobs.containsKey(bfileName) && (splitCommit.fileBlobs.containsKey(bfileName)
                    && !splitCommit.fileBlobs.get(bfileName).equals(bfileID))){
                conflict = true;
                conflict(null, bfileID, bfileName);
            }

            if(curr.fileBlobs.containsKey(bfileName)  && !curr.fileBlobs.get(bfileName).equals(bfileID)){
                if(!splitCommit.fileBlobs.containsKey(bfileName)){
                    conflict(curr.fileBlobs.get(bfileName), bfileID, bfileName);
                    conflict = true;
                }

                else if(!curr.fileBlobs.get(bfileName).equals(splitCommit.fileBlobs.get(bfileName))
                        && splitCommit.fileBlobs.containsKey(bfileName) && !bfileID.equals((splitCommit.fileBlobs.get(bfileName)))) {
                    conflict(curr.fileBlobs.get(bfileName), bfileID, bfileName);
                    conflict = true;
                }
            }
            Utils.writeObject(STAGING_FILE, stagingArea);
        }

        commit("Merged " + branchName + " into " + commitPointers.readHeadCommit()[0] + ".");

        //add branch commit to parentID of new commit
        String newcommitID = commitPointers.readHeadCommit()[1];
        File madecFile = Utils.join(Commit.COMMIT_FOLDER, newcommitID + ".txt");
        Commit newCommit = Utils.readObject(madecFile, Commit.class);
        newCommit.parentID.add(1, branchCommitID);
        newCommit.saveCommit(newcommitID);

        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private static void conflict(String currBlobFileID, String branchBlobFileID, String fileName) throws IOException {
        File cfile = Utils.join(Blob.BLOB_FOLDER, currBlobFileID + ".txt");
        File bfile = Utils.join(Blob.BLOB_FOLDER, branchBlobFileID + ".txt");
        File cwdFile = Utils.join(CWD, fileName);

        String cBlobContents = "";
        String bBlobContents = "";

        if (cfile.exists()) {
            Blob cBlobObj = Utils.readObject(cfile, Blob.class );
            cBlobContents = cBlobObj.contents;
        }
        if (bfile.exists()) {
            Blob bBlobObj = Utils.readObject(bfile, Blob.class );
            bBlobContents = bBlobObj.contents;
        }

        String newContents = "<<<<<<< HEAD\n" + cBlobContents + "=======\n" +bBlobContents + ">>>>>>>\n";
        Utils.writeContents(cwdFile, newContents);
        add(fileName);
    }
    private static void addRemote(String remoteName, String remoteDir){


    }

    private static String splitPointFinder(String givenBranch, String currName) {

        String splitPoint = "null";

        String currBranchName = commitPointers.branches.get(givenBranch);
        File bFile = Utils.join(Commit.COMMIT_FOLDER, currBranchName + ".txt");
        Commit currBranch = Utils.readObject(bFile, Commit.class);

        File cFile = Utils.join(Commit.COMMIT_FOLDER, currName + ".txt");
        Commit curr = Utils.readObject(cFile, Commit.class);

        boolean found = false;

        while (currName != null) {
            //resets branch name to first
            currBranchName = commitPointers.branches.get(givenBranch);
            File newbFile = Utils.join(Commit.COMMIT_FOLDER, currBranchName + ".txt");
            currBranch = Utils.readObject(newbFile, Commit.class);

            while (currBranchName != null) {

                if (currName.equals(currBranchName)) {
                    splitPoint = currBranchName;
                    found = true;
                    break;
                }

                if((currBranch.parentID.size() > 1 && currName.equals(currBranch.parentID.get(1)))){
                    splitPoint = currName;
                    found = true;
                    break;
                }

                //Chached to currBranchName???
                if (curr.parentID.size() > 1 && curr.parentID.get(1).equals(currBranchName)){
                    splitPoint = currBranchName;
                    found = true;
                    break;
                }


                //TODO: NO CURR BRANCH NAME
                currBranchName = currBranch.parentID.get(0);

                if (currBranchName != null) {
                    File newcFile = Utils.join(Commit.COMMIT_FOLDER, currBranchName + ".txt");
                    currBranch = Utils.readObject(newcFile, Commit.class);
                }
            }
            if (found) {
                break;
            }

            currName = curr.parentID.get(0);

            if (currName != null) {
                File newcFile = Utils.join(Commit.COMMIT_FOLDER, currName + ".txt");
                curr = Utils.readObject(newcFile, Commit.class);
            }
        }
        return splitPoint;
    }

}

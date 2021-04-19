package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class Blob implements Serializable {


    static final File CWD = new File(".");
    static final File GITLET_FOLDER = new File(CWD,".gitlet");
    static final File BLOB_FOLDER = new File(GITLET_FOLDER, "blob");
    private String name;
    public String contents;

    public Blob(String title, File file) {
        BLOB_FOLDER.mkdir();
        name = title;
        contents = Utils.readContentsAsString(file);
    }

    public void saveBlob(String blobID) throws IOException {
        File blobFile = Utils.join(BLOB_FOLDER, blobID + ".txt");
        blobFile.createNewFile();
        Utils.writeObject(blobFile, this);
    }
}

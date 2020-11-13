package Encoding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import com.google.common.hash.*;

public class Decode {

    private String rootDirectory;
    private String fileName;
    private LinkedHashMap<String, String> metaDataHash;
    private String merkleRoot;

    public Decode(String rootDirectory, String fileName, LinkedHashMap<String, String> metaDataHash,
            String merkleRoot) {
        this.rootDirectory = rootDirectory;
        this.fileName = fileName;
        this.metaDataHash = metaDataHash;
        this.merkleRoot = merkleRoot;
    }

    public void merge() {

        String pathBasic = Paths.get(this.rootDirectory, "fragment").toString();
        String workDir = Paths.get(pathBasic, this.fileName).toString();
        String mergeLocPath = Paths.get(this.rootDirectory, "files").toString();
        File finalFile = Paths.get(mergeLocPath, this.fileName).toFile();
        FileOutputStream finalFileOutputStream = null;
        Boolean r = null;
        try {
            r = finalFile.createNewFile();
            finalFileOutputStream = new FileOutputStream(finalFile);
        } catch (Exception e) {
            System.out.println("File creation Failed");
            return;
        }
        if (r) {
            for (Integer i = 0; i < metaDataHash.size(); i++) {
                String contentHash = metaDataHash.get(Integer.toString(i));

                try {

                    File contentFile = Paths.get(workDir, contentHash).toFile();
                    FileInputStream contentFileInputStream = new FileInputStream(contentFile);
                    byte[] content = new byte[(int) contentFile.length()];

                    contentFileInputStream.read(content);
                    contentFileInputStream.close();

                    finalFileOutputStream.write(content);

                } catch (FileNotFoundException e) {

                    System.out.println("Fragement lost");
                    return;

                } catch (IOException e) {

                    System.out.println("Fragement read or write failed");
                    return;
                } catch (Exception e) {
                    System.out.println("Merge Error");
                    return;
                }

            }

            try {
                finalFileOutputStream.close();
            } catch (Exception e) {
                System.out.println("Final File write failed");
            }
            System.out.println("File successfully merged");

        } else {

            try {
                finalFileOutputStream.close();
            } catch (Exception e) {
                System.out.println("Final File write failed");
            }
            System.out.println("File Creation Failed");
        }

    }

    /*
     * public Boolean integrityCheck() {
     * 
     * }
     */
}

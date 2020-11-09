package Encoding;

import java.util.HashMap;

import com.google.common.hash.*;

import java.nio.*;
import java.nio.file.Paths;
import java.io.*;
import java.util.Map.Entry;
import java.nio.charset.StandardCharsets;

public class Encode {

    private HashMap<String, String> metaDataHash = new HashMap<String, String>();
    private String rootDirectory = null, fileName = null;
    private File file = null;
    private float fileSize = 0;

    public Encode(String rootDirectory, String fileName) {

        this.rootDirectory = rootDirectory;
        this.fileName = fileName;

        String fileDirectory = Paths.get(rootDirectory, "files").toString();
        this.file = new File(Paths.get(fileDirectory, fileName).toString());

    }

    public void split() {
        long fileLength = this.file.length();
        this.fileSize = (float) fileLength / (1024 * 1024);
        long pieceCount = fileLength / (1024 * 1024);
        long fragmentSize = fileLength % (1024 * 1024);

        String fragmentPath = Paths.get(rootDirectory, "fragment").toString();
        String fragmentDir = Paths.get(fragmentPath, this.fileName).toString();
        File fragmentWorkdir = Paths.get(fragmentPath, this.fileName).toFile();
        fragmentWorkdir.mkdirs();

        long index = 0;

        try {
            FileInputStream fileReader = new FileInputStream(this.file);

            while (pieceCount > 0) {

                byte[] piece = new byte[1024 * 1024];
                fileReader.read(piece);

                String hash = Hashing.sha256().hashBytes(piece).toString();
                this.writePiece(hash, fragmentDir, piece);
                metaDataHash.put(Long.toString(index), hash);
                index++;
                pieceCount--;

            }

            String merkleRoot = createMerkleRoot();
            metaDataHash.put("merkleRoot", merkleRoot);
            metaDataHash.put("fileName", this.fileName);
            metaDataHash.put("fileSize", Float.toString(this.fileSize));
            this.createMetaDataFile();

            fileReader.close();

            System.out.println("MetaFile Created Use it");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String createMerkleRoot() {
        String merkleRoot = "";

        for (Entry<String, String> entry : this.metaDataHash.entrySet()) {
            merkleRoot = Hashing.sha256().hashString(merkleRoot + entry.getValue(), StandardCharsets.UTF_8).toString();
        }
        return merkleRoot;
    }

    public void writePiece(String hash, String fragmentDir, byte[] piece) {
        File fragment = Paths.get(fragmentDir, hash).toFile();
        try {
            FileOutputStream fragmentWriter = new FileOutputStream(fragment);
            fragmentWriter.write(piece);
            fragmentWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createMetaDataFile() {

        String metaPath = Paths.get(this.rootDirectory, "metaFile").toString();
        String metaFileName = this.fileName + ".metaData";
        File metaFile = Paths.get(metaPath, metaFileName).toFile();

        try {
            ObjectOutputStream fileWriter = new ObjectOutputStream(new FileOutputStream(metaFile));

            fileWriter.writeObject(this.metaDataHash);
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("MetaData file creation Failed");
            e.printStackTrace();
        }

    }

}

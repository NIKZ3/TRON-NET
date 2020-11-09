package Leecher;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;
import java.net.*;
import Communication.Request.*;

public class Leecher implements Runnable {

    private String merkleRoot = null;
    private HashMap<String, String> metaDataHash = new HashMap<String, String>();
    private String rootDirectory = null;
    private String trackerIp = null;
    private Integer trackerPort = null;
    private String filename = null;
    private String metaDataFileName = null;
    private ArrayList<Socket> seeders = new ArrayList<Socket>();
    private Socket trackerSocket;
    private ObjectOutputStream trackerOutputStream = null;
    private String ipAddress = null;
    private Integer portNo = null;
    private long pendingPieces = 0;
    private Float fileSize = 0;

    // TODO: MetaData Read and get MerkleRoot and hash of pieces
    public Leecher(String rootDirectory, String filename, String metaDataFileName) {

        this.ipAddress = "localhost";
        this.portNo = 3000;

        this.rootDirectory = rootDirectory;
        this.filename = this.filename;
        this.metaDataFileName = this.metaDataFileName;

        String metaFileRoot = Paths.get(rootDirectory, "metaFile").toString();
        String metaFilePath = Paths.get(metaFileRoot, metaDataFileName).toString();
        File metaFile = new File(metaFilePath);

        try {
            ObjectInputStream metaFileReader = new ObjectInputStream(new FileInputStream(metaFile));
            this.metaDataHash = (HashMap<String, String>) metaFileReader.readObject();
        } catch (Exception e) {
            System.out.println("MetaFile Read Error");
            e.printStackTrace();
        }

        this.merkleRoot = metaDataHash.get("merkleRoot");
        this.filename = metaDataHash.get("fileName");
        this.fileSize = Float.parseFloat(metaDataHash.get("fileSize"));
        metaDataHash.remove("merkleRoot");
        metaDataHash.remove("fileName");
        metaDataHash.remove("fileSize");

        this.pendingPieces = metaDataHash.size();

    }

    public void connectToTracker() {
        try {
            Socket socket = new Socket(this.trackerIp, this.trackerPort);
            this.trackerSocket = socket;
            this.trackerOutputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            System.out.println("Connection to Tracker Failed");
            e.printStackTrace();
        }
    }

    public void leechRequest(String merkleRoot) {
        leechRequest leechReq = new leechRequest(this.merkleRoot, this.ipAddress, this.portNo);
        try {
            trackerOutputStream.writeObject(leechReq);
        } catch (IOException e) {
            System.out.println("Write to tracker Failed");
            e.printStackTrace();
        }
        System.out.println("Leech Request Sent to tracker");
    }

    public void listenToSeeders() {

        try {
            while (this.pendingPieces != 0) {
                ServerSocket serverSocket = new ServerSocket(this.portNo);
                Socket socket = serverSocket.accept();
                seeders.add(socket);

                new Thread() {
                    public void run() {
                        extractSeed();
                    }
                }.start();

            }

        } catch (Exception e) {
            System.out.println("ServerSocketCreation Failed");
            e.printStackTrace();
        }

    }

    public void extractSeed() {

    }

    public void assignFragment() {

    }

    @Override
    public void run() {

        connectToTracker();
        leechRequest(this.merkleRoot);
        System.out.println("Connected to Tracker");
        new Thread() {
            public void run() {
                listenToSeeders();
            }
        }.start();
    }

}

package Leecher;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.io.*;
import java.net.*;
import Communication.Request.*;
import Communication.p2p.*;

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
    private long pendingPiecesCount = 0;
    private Float fileSize = 0.0f;
    private HashMap<Integer, String> pendingPieces = new HashMap<Integer, String>();
    private ArrayList<Integer> pendingIndexes = new ArrayList<Integer>();
    private ArrayList<Integer> assignmentIndexes = new ArrayList<Integer>();

    private Integer fileIndex = 0;

    // TODO: Using synchronise block for indexes
    // TODO: disconnect upon succesful recepetion of all files
    // TODO: thread pools for seeders
    // TODO: Handle seeder disconnection
    // TODO:- Add check wherein if pending pieces have become zero we end the
    // TODO:- connection i.e. interrupt the thread
    // ! Synchronized blocks might create issue for other loops using same variables
    // !in loop

    public Leecher(String rootDirectory, String filename, String metaDataFileName) {

        this.ipAddress = "localhost";
        this.portNo = 3000;

        this.rootDirectory = rootDirectory;
        this.filename = filename;
        this.metaDataFileName = metaDataFileName;

        String metaFileRoot = Paths.get(rootDirectory, "metaFile").toString();
        String metaFilePath = Paths.get(metaFileRoot, metaDataFileName).toString();
        File metaFile = new File(metaFilePath);

        try {
            ObjectInputStream metaFileReader = new ObjectInputStream(new FileInputStream(metaFile));
            this.metaDataHash = (HashMap<String, String>) metaFileReader.readObject();
            metaFileReader.close();
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

        String fileDirectoryBasic = Paths.get(rootDirectory, "fragment").toString();
        File fileDirectory = Paths.get(fileDirectoryBasic, this.filename).toFile();
        fileDirectory.mkdirs();

        this.pendingPiecesCount = metaDataHash.size();
        Integer ind = 0;
        for (Entry<String, String> entry : this.metaDataHash.entrySet()) {
            this.pendingPieces.put(ind, entry.getValue());
            this.pendingIndexes.add(ind);
            this.assignmentIndexes.add(ind);
            ind++;
        }

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
            ServerSocket serverSocket = new ServerSocket(this.portNo);
            while (this.pendingPiecesCount != 0) {
                Socket socket = serverSocket.accept();
                seeders.add(socket);

                new Thread() {
                    public void run() {
                        extractSeed(socket);
                    }
                }.start();

            }

            this.disconnectSeeders();
            serverSocket.close();

        } catch (Exception e) {
            System.out.println("ServerSocketCreation Failed");
            e.printStackTrace();
        }

        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void extractSeed(Socket socket) {
        Integer distributionIndex = -1;
        try {
            while (this.pendingPiecesCount != 0) {
                ObjectInputStream seedInputStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream seedOutputStream = new ObjectOutputStream(socket.getOutputStream());

                distributionIndex = this.BalanceDistribution();
                while (distributionIndex == -1) {

                    Thread.sleep(500);
                    distributionIndex = this.BalanceDistribution();
                }
                // TODO:- Add check wherein if pending pieces have become zero we end the
                // TODO:- connection i.e. interrupt the thread
                this.sendDistributionMessage(distributionIndex, seedOutputStream);

                // ! Notifies seed disconnect;
                p2p msg = (p2p) seedInputStream.readObject();
                if (msg.getMsgType().equals("SEED")) {
                    seedData data = (seedData) msg;
                    byte[] pieceContent = data.getContent();
                    String contentHash = data.getContentHash();

                    this.writePieceToDisk(pieceContent, contentHash, distributionIndex);
                } else if (msg.getMsgType().equals("DISCONNECT")) {

                    seeders.remove(socket);
                    System.out.println("SEEDER DISCONNECTED");
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("seeder Disconnected");
            this.seeders.remove(socket);
            this.reintroduceIndex(distributionIndex);
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void sendDistributionMessage(Integer distributionIndex, ObjectOutputStream seedOutputStream) {

        try {
            String contentHash = pendingPieces.get(distributionIndex);
            distributionMessage dmg = new distributionMessage(distributionIndex, contentHash);
            seedOutputStream.writeObject(dmg);
        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    public Integer BalanceDistribution() {

        // !SHARED VARIABLE
        synchronized (this) {
            if (assignmentIndexes.size() != 0) {
                Integer ind = assignmentIndexes.get(0);
                assignmentIndexes.remove(0);

                return ind;
            }

            else
                return -1;
        }
    }

    public void reintroduceIndex(Integer distributionIndex) {

        // !SHARED VARIABLE
        synchronized (this) {
            assignmentIndexes.add(distributionIndex);
        }

    }

    public void writePieceToDisk(byte[] content, String contentHash, Integer distributionIndex) {

        String basicPath = Paths.get(rootDirectory, "fragment").toString();
        String fragmentPath = Paths.get(basicPath, this.filename).toString();

        try {
            File fragmentFile = Paths.get(fragmentPath, contentHash).toFile();
            fragmentFile.createNewFile();
            FileOutputStream fragmentFileWriter = new FileOutputStream(fragmentFile);
            fragmentFileWriter.write(content);
            fragmentFileWriter.close();

            this.updatePendingPieces(distributionIndex);
        } catch (Exception e) {
            System.out.print("Fragment Write Failed:-");
            System.out.println(contentHash);
            e.printStackTrace();

        }
    }

    public void updatePendingPieces(Integer distributionIndex) {

        // ! Synchronize block for shared variable
        synchronized (this) {
            this.pendingPieces.remove(distributionIndex);
            this.pendingPiecesCount--;
            System.out.println("UPATED PENDING PIECES:-" + distributionIndex);
        }
    }

    public void disconnectSeeders() {

        for (Socket socket : this.seeders) {
            try {
                ObjectOutputStream seedOutputStream = new ObjectOutputStream(socket.getOutputStream());
                disconnect disCon = new disconnect();
                seedOutputStream.writeObject(disCon);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

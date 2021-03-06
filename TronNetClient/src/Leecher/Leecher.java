package Leecher;

import java.nio.channels.Pipe.SinkChannel;
import java.nio.file.Paths;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.io.*;
import java.net.*;
import Communication.Request.*;
import Communication.p2p.*;
import com.google.common.hash.*;
import Encoding.*;

public class Leecher implements Runnable {

    private String merkleRoot = null;
    private LinkedHashMap<String, String> metaDataHash = new LinkedHashMap<String, String>();
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
    private LinkedHashMap<Integer, String> pendingPieces = new LinkedHashMap<Integer, String>();
    private ArrayList<Integer> pendingIndexes = new ArrayList<Integer>();
    private ArrayList<Integer> assignmentIndexes = new ArrayList<Integer>();

    private Integer fileIndex = 0;

    // TODO: thread pools for seeders

    // TODO:- After creation of file fragments replace name by merkleroot

    public Leecher(String rootDirectory, String filename, String metaDataFileName) {

        this.ipAddress = "localhost";
        this.portNo = 3000;
        this.trackerPort = 8080;
        this.trackerIp = "localhost";
        this.rootDirectory = rootDirectory;
        this.filename = filename;
        this.metaDataFileName = metaDataFileName;

        String metaFileRoot = Paths.get(rootDirectory, "metaFile").toString();
        String metaFilePath = Paths.get(metaFileRoot, metaDataFileName).toString();
        File metaFile = new File(metaFilePath);

        try {
            ObjectInputStream metaFileReader = new ObjectInputStream(new FileInputStream(metaFile));
            this.metaDataHash = (LinkedHashMap<String, String>) metaFileReader.readObject();
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
        leechRequest leechReq = new leechRequest(this.merkleRoot, this.ipAddress, this.filename, this.portNo);
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
                        System.out.println("Seed Extraction Complete");
                        try {
                            if (pendingPiecesCount == 0) {
                                serverSocket.close();
                            }
                        } catch (Exception e) {
                            System.out.println("Seeding complete Closed");
                        }
                        return;
                    }
                }.start();

            }
            /*
             * System.out.println("Out of listen seeder"); this.disconnectSeeders();
             * System.out.println("Closing Leecher"); serverSocket.close();
             */

        } catch (Exception e) {
            System.out.println("ServerSocket Closed");

            // e.printStackTrace();
            return;

        }

        return;

    }

    public void extractSeed(Socket socket) {
        Integer distributionIndex = -1;
        try {
            ObjectInputStream seedInputStream = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream seedOutputStream = new ObjectOutputStream(socket.getOutputStream());

            while (this.pendingPiecesCount != 0) {
                distributionIndex = this.BalanceDistribution();
                while (distributionIndex == -1) {

                    Thread.sleep(500);
                    if (this.pendingPiecesCount == 0) {
                        break;
                    }
                    distributionIndex = this.BalanceDistribution();
                }

                if (this.pendingPiecesCount == 0) {

                    seedInputStream.close();
                    seedOutputStream.close();
                    break;
                }
                this.sendDistributionMessage(distributionIndex, seedOutputStream);

                // ! Notifies seed disconnect;
                p2p msg = (p2p) seedInputStream.readObject();
                if (msg.getMsgType().equals("SEED")) {
                    seedData data = (seedData) msg;
                    byte[] pieceContent = data.getContent();
                    // String contentHash = data.getContentHash();
                    String contentHash = pendingPieces.get(data.getDistributionIndex());

                    this.writePieceToDisk(pieceContent, contentHash, distributionIndex);
                } else if (msg.getMsgType().equals("DISCONNECT")) {

                    seeders.remove(socket);
                    System.out.println("SEEDER DISCONNECTED");
                    this.reintroduceIndex(distributionIndex);
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

        return;

    }

    public void sendDistributionMessage(Integer distributionIndex, ObjectOutputStream seedOutputStream) {

        try {
            System.out.println("Distribution message:---" + distributionIndex);
            String contentHash = pendingPieces.get(distributionIndex);
            distributionMessage dmg = new distributionMessage(distributionIndex, contentHash);
            seedOutputStream.writeObject(dmg);
        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    public synchronized Integer BalanceDistribution() {

        // !SHARED VARIABLE

        if (assignmentIndexes.size() != 0) {
            Integer ind = assignmentIndexes.get(0);
            assignmentIndexes.remove(0);

            return ind;
        }

        else
            return -1;
    }

    public synchronized void reintroduceIndex(Integer distributionIndex) {

        // !SHARED VARIABLE

        assignmentIndexes.add(distributionIndex);

    }

    public void writePieceToDisk(byte[] content, String contentHash, Integer distributionIndex) {

        // !DIRECTORY NAME CHANGET TO FRAGMENTR

        String hash = Hashing.sha256().hashBytes(content).toString();

        if (hash.equals(pendingPieces.get(distributionIndex))) {
            String basicPath = Paths.get(rootDirectory, "fragment").toString();
            String fragmentPath = Paths.get(basicPath, this.filename).toString();
            File fragmentDir = Paths.get(basicPath, this.filename).toFile();

            if (!fragmentDir.exists()) {
                fragmentDir.mkdirs();
            }

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

        else {

            System.out.println("Corrupt Fragment:- " + hash);
            this.reintroduceIndex(distributionIndex);
        }
    }

    public synchronized void updatePendingPieces(Integer distributionIndex) {

        // ! Synchronize block for shared variable

        this.pendingPieces.remove(distributionIndex);
        this.pendingPiecesCount--;
        System.out.println("UPATED PENDING PIECES:-" + distributionIndex);

        if (this.pendingPiecesCount == 0) {
            this.disconnectSeeders();
        }

    }

    public void disconnectSeeders() {

        for (Socket socket : this.seeders) {
            try {
                System.out.println("Disconnecting Seeders");
                // ObjectOutputStream seedOutputStream = new
                // ObjectOutputStream(socket.getOutputStream());
                /*
                 * disconnect disCon = new disconnect(); seedOutputStream.writeObject(disCon);
                 */
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void finalMerge() {
        Decode decode = new Decode(this.rootDirectory, this.filename, this.metaDataHash, this.merkleRoot);
        decode.merge();
    }

    public long getPendingPiecesCount() {
        return this.pendingPiecesCount;
    }

    public int getProgress() {

        int original = this.metaDataHash.size();
        int pending = (int) this.pendingPiecesCount;

        int completion = (pending / original) * 100;
        return (100 - completion);

    }

    @Override
    public void run() {

        connectToTracker();
        leechRequest(this.merkleRoot);
        System.out.println("Connected to Tracker");
        listenToSeeders();

        return;
        /*
         * new Thread() { public void run() { listenToSeeders(); } }.start();
         */
    }

}

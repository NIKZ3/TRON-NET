package Seeder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import Communication.Request.*;
import Communication.p2p.*;

public class Seeder implements Runnable {

    private String trackerIp = null;
    private Integer trackerPort = null;
    private String ipAddress = null;
    private Integer portNo = null;
    private String rootDirectory = null;
    private Socket trackerSocket = null;
    private ObjectOutputStream trackerOutputStream = null;
    private ObjectInputStream trackerInputStream = null;
    private ArrayList<String> existingFiles = new ArrayList<String>();

    public Seeder(String rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.ipAddress = "localhost";
        this.portNo = 3001;
        this.trackerIp = "localhost";
        this.trackerPort = 8080;
    }

    public void connectToTracker() {
        try {
            Socket socket = new Socket(this.trackerIp, this.trackerPort);
            this.trackerSocket = socket;
            this.createSeedRequest();
            System.out.println("After create seed request");
        } catch (Exception e) {
            System.out.println("Connection with tracker Failed");
            e.printStackTrace();
        }
    }

    public void createSeedRequest() {
        try {
            this.trackerOutputStream = new ObjectOutputStream(this.trackerSocket.getOutputStream());
            seedRequest seedR = new seedRequest();
            trackerOutputStream.writeObject(seedR);
            System.out.println("SEEDER ANNOUNCED");
            this.listenToTracker();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listenToTracker() {

        System.out.println("Waiting for seed requests");
        try {

            this.trackerInputStream = new ObjectInputStream(this.trackerSocket.getInputStream());
            while (true) {

                System.out.println("Waiting for seed requests");
                serverSeedMsg serverMsg = (serverSeedMsg) trackerInputStream.readObject();
                String merkleRoot = serverMsg.getMerkleRoot();
                String fileName = serverMsg.getFileName();
                if (existingFiles.contains(fileName)) {
                    System.out.println("File Available Starting seed");
                    this.seedFile(serverMsg);
                }

            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Tracker not online");
            e.printStackTrace();
        }
    }

    public void seedFile(serverSeedMsg serverMsg) {
        try {
            Socket socket = new Socket(serverMsg.getLeecherIP(), serverMsg.getLeecherPort());

            ObjectOutputStream leecherOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream leecherInputStream = new ObjectInputStream(socket.getInputStream());

            String filePathBasic = Paths.get(this.rootDirectory, "fragment").toString();
            String filePath = Paths.get(filePathBasic, serverMsg.getFileName()).toString();

            while (true) {
                p2p msg = (p2p) leecherInputStream.readObject();
                String msgType = msg.getMsgType();
                System.out.println(msgType);
                if (msgType.equals("DISTRIBUTION")) {

                    distributionMessage dmg = (distributionMessage) msg;
                    String contentHash = dmg.getContentHash();
                    Integer distributionIndex = dmg.getDistributionIndex();
                    System.out.println(contentHash + ":::::-----" + distributionIndex);
                    this.initiateSeed(contentHash, distributionIndex, filePath, leecherOutputStream);
                } else if (msgType.equals("DISCONNECT")) {
                    System.out.println("YOO");
                    socket.close();
                    break;
                }
            }

        } catch (Exception e) {
            System.out.println("Leecher is offline");
            e.printStackTrace();

            return;
        }

        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initiateSeed(String contentHash, Integer distributionIndex, String filePath,
            ObjectOutputStream leechOutputStream) {
        try {
            File contentFile = Paths.get(filePath, contentHash).toFile();
            byte[] content = new byte[(int) contentFile.length()];
            System.out.println(contentFile.exists());
            FileInputStream contentFileInputStream = new FileInputStream(contentFile);

            contentFileInputStream.read(content);
            seedData data = new seedData(content, distributionIndex);
            leechOutputStream.writeObject(data);
            contentFileInputStream.close();
        } catch (IOException e) {
            System.out.println(contentHash + "+-----------------" + "Corrupt or leecher offline");
            e.printStackTrace();
        }

    }

    public void checkAvailableFiles() {

        File file = Paths.get(this.rootDirectory, "files").toFile();

        File[] listOfFiles = file.listFiles();

        for (File f : listOfFiles) {
            existingFiles.add(f.getName());
        }
    }

    @Override
    public void run() {

        // serverSeedMsg serverMsg = new serverSeedMsg("111", "localhost", 3000,
        // "xyz.mp3");
        // seedFile(serverMsg);
        System.out.println("Before Tracker");
        checkAvailableFiles();
        connectToTracker();
        System.out.println("After Tracker");

    }

}

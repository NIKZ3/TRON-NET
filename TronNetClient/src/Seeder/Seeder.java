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
import java.util.HashMap;
import Communication.Request.*;
import Communication.p2p.*;

//TODO:- Check directory and add merkles in map
//TODO:- Connect to tracker and then listen to tracker once seed request made start seeding

public class Seeder implements Runnable {

    private String trackerIp = null;
    private Integer trackerPort = null;
    private String ipAddress = null;
    private Integer portNo = null;
    private String rootDirectory = null;
    private Socket trackerSocket = null;
    private ObjectOutputStream trackeOutputStream = null;
    private ObjectInputStream trackerInputStream = null;
    private HashMap<String, String> availableFiles = new HashMap<String, String>();

    public Seeder(String rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.ipAddress = "localhost";
        this.portNo = 3001;
    }

    public void connectToTracker() {
        try {
            Socket socket = new Socket(this.trackerIp, this.trackerPort);
            this.trackerSocket = socket;
            this.trackeOutputStream = new ObjectOutputStream(socket.getOutputStream());
            this.trackerInputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("Connection with tracker Failed");
            e.printStackTrace();
        }
    }

    public void listenToTracker() {

        try {
            while (true) {

                System.out.println("Waiting for seed requests");
                serverSeedMsg serverMsg = (serverSeedMsg) trackerInputStream.readObject();
                String merkleRoot = serverMsg.getMerkleRoot();
                if (availableFiles.containsKey(merkleRoot)) {
                    System.out.println("Merkle present seeding Initiated");
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

            /*
             * String filePathBasic = Paths.get(this.rootDirectory, "fragments").toString();
             * String filePath = Paths.get(filePathBasic,
             * serverMsg.getFileName()).toString(); File seedFile = Paths.get(filePath,
             * serverMsg.getMerkleRoot()).toFile(); FileInputStream seedFileInputStream =
             * new FileInputStream(seedFile); byte[] pieceContent = new byte[(int)
             * seedFile.length()]; seedFileInputStream.read(pieceContent);
             */

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
            seedData data = new seedData(content, distributionIndex, contentHash);
            leechOutputStream.writeObject(data);

        } catch (IOException e) {
            System.out.println(contentHash + "+-----------------" + "Corrupt or leecher offline");
            e.printStackTrace();
        }

    }

    @Override
    public void run() {

        serverSeedMsg serverMsg = new serverSeedMsg("111", "localhost", 3000, "xyz.mp3");
        seedFile(serverMsg);
        // connectToTracker();

    }

}

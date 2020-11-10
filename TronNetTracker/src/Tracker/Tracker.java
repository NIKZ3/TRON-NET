package Tracker;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import Request.Request;
import Request.leechRequest;
import Request.seedRequest;
import Request.serverSeedMsg;

public class Tracker {

    private Integer portNo = null;

    private ServerSocket serverSocket = null;
    private ArrayList<Socket> listPeers = new ArrayList<Socket>();
    private ArrayList<Socket> listSeeders = new ArrayList<Socket>();

    public Tracker(Integer portNo) {
        this.portNo = portNo;
        try {
            this.serverSocket = new ServerSocket(this.portNo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() {
        while (true) {
            try {
                Socket socket = this.serverSocket.accept();
                listPeers.add(socket);
                new Thread() {
                    public void run() {
                        listenToPeer(socket);
                    }
                }.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void listenToPeer(Socket socket) {
        try {
            ObjectInputStream peerInputStream = new ObjectInputStream(socket.getInputStream());
            Request request = (Request) peerInputStream.readObject();

            while (true) {

                Request request = (Request) peerInputStream.readObject();
                if (request.getRequestType().equals("LEECH")) {
                    this.processLeechRequest(request);
                } else if (request.getRequestType().equals("SEED")) {
                    listSeeders.add(socket);
                }
            }

        } catch (ClassNotFoundException | IOException e) {
            System.out.println("SEEDER OR LEECHER REMOVED");
            listPeers.remove(socket);
            listSeeders.remove(socket);
            e.printStackTrace();
        }
    }

    public void processLeechRequest(Request request) {
        leechRequest LeechRequest = (leechRequest) request;

        serverSeedMsg seedMsg = new serverSeedMsg(LeechRequest.getMerkleRoot(), LeechRequest.getIpAddress(),
                LeechRequest.getPortNo(), LeechRequest.getFileName());

        for (Socket socket : listSeeders) {
            try {
                ObjectOutputStream seedWriter = new ObjectOutputStream(socket.getOutputStream());
                seedWriter.writeObject(seedMsg);
                seedWriter.close();
            } catch (Exception e) {
                System.out.println("Write Failed");
                e.printStackTrace();
            }
        }

        System.out.println("Seed Request PRocessed");

    }

}

import Encoding.Encode;
import Leecher.Leecher;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello, World!");

        // Encode encode = new Encode("/home/nikhil/p2p", "xyz.mp3");
        // encode.split();

        Leecher leecher = new Leecher("/home/nikhil/p2p", "xyz.mp3", "xyz.mp3.metaData");
        new Thread(leecher).start();

        // Seeder seeder = new Seeder("/home/nikhil/p2p");
        // new Thread(seeder).start();

        // Tracker tracker = new Tracker(3002);
        // tracker.startServer();

    }
}

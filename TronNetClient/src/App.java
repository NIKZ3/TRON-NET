import Encoding.Encode;
import Leecher.Leecher;
import Seeder.Seeder;
import java.util.Scanner;

//TODO:-Add support to seed multiple files at a time

public class App {
    public static void main(String[] args) throws Exception {

        Scanner in = new Scanner(System.in);

        String input = null;

        System.out.println("1.Seeder\n2.Leecher\n3.Encode a File");

        input = in.nextLine();
        int x = Integer.parseInt(input);
        if (x == 1) {

            String rootDirectory = null;
            System.out.println("Enter root directory Path");

            rootDirectory = in.nextLine();

            Seeder seeder = new Seeder(rootDirectory);
            new Thread(seeder).start();

        } else if (x == 2) {

            String rootDirectory = null;
            String fileName = null;
            String metaDataName = null;

            System.out.println("Enter root directory Path");
            rootDirectory = in.nextLine();

            System.out.println("Enter File Name");
            fileName = in.nextLine();

            System.out.println("Enter MetaData Name");
            metaDataName = in.nextLine();

            // Leecher leecher = new Leecher("/home/nikhil/p2p", "xyz.mp3",
            // "xyz.mp3.metaData");
            Leecher leecher = new Leecher(rootDirectory, fileName, metaDataName);
            new Thread(leecher).start();

            while (leecher.getPendingPiecesCount() != 0) {
                System.out.println("Progress of File Transfer:-" + leecher.getProgress() + "%");
            }

        } else if (x == 3) {

            String rootDirectory = null;
            String fileName = null;
            String metaDataName = null;

            System.out.println("Enter root directory Path");
            rootDirectory = in.nextLine();

            System.out.println("Enter File Name");
            fileName = in.nextLine();

            // Encode encode = new Encode("/home/nikhil/p2p", "xyz.mp3");
            Encode encode = new Encode(rootDirectory, fileName);
            encode.split();

        }

        // Encode encode = new Encode("/home/nikhil/p2p", "xyz.mp3");
        // encode.split();

        // Leecher leecher = new Leecher("/home/nikhil/p2p", "xyz.mp3",
        // "xyz.mp3.metaData");
        // new Thread(leecher).start();

        // Tracker tracker = new Tracker(3002);
        // tracker.startServer();

    }
}

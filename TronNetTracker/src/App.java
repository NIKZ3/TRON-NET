import Tracker.Tracker;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello, World!");

        Tracker tracker = new Tracker(3002);
        tracker.startServer();

    }
}

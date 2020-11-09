import Encoding.Encode;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello, World!");

        Encode encode = new Encode("/home/nikhil/p2p", "xyz.mp3");
        encode.split();
    }
}

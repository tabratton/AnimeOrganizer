import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Vector;

public class Main {
  public static ArrayList<String> moved = new ArrayList<>();
  public static ArrayList<String> detected = new ArrayList<>();
  public static long id = 1;
  public static String prefix = "[%s - ID %d] %s thread: %s";

  private static final int WAIT_TIME = 10_000;

  public static void main(String[] args) {
    Vector<Thread> threads = new Vector<>();

    JSONObject parser = new JSONObject(System.getProperty("user.dir"
        + File.separator + "paths.json"));
    JSONArray paths = parser.getJSONArray("paths");

    for (int i = 0; i < paths.length(); i++) {
      JSONObject currentPath = paths.getJSONObject(i);
      String source = currentPath.getString("source");
      String destination = currentPath.getString("destination");
      boolean placeInSub = currentPath.getBoolean("placeInSub");

      source = source.replaceAll("/", File.separator);
      destination = destination.replaceAll("/", File.separator);

      Watcher watcher = new Watcher(Paths.get(source), Paths.get(destination),
          placeInSub, WAIT_TIME, id++);
      threads.add(new Thread(watcher));
    }

    for (Thread thread : threads) {
      thread.start();
    }
  }

}

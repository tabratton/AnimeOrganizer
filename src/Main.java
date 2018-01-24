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
  private static Vector<Thread> threads = new Vector<>();
  public static String prefix = "[%s - ID %d] %s thread: %s";
  private static char sep = File.separatorChar;

  private static String baseDestination =
      String.format("J:%sDownloads%sSeedbox%s", sep, sep, sep);
  private static String baseSource = String.format("Z:%sSeedbox%s", sep, sep);


  private static String rssSource = String.format(baseSource, "RSSFeed");
  private static String rssDestination = "J:" + File.separator + "Anime" + File
      .separator + "Airing Series";

  private static String musicSource = String.format(baseSource, "Music");
  private static String musicDestination = String.format(baseDestination,
      "Music");

  private static String westernSource = String.format(baseSource,
      "WesternMedia");
  private static String westernDestination = String.format(baseDestination,
      "WesternMedia");

  private static String mangaSource = String.format(baseSource, "Manga");
  private static String mangaDestination = String.format(baseDestination,
      "Manga");

  private static String animeSource = String.format(baseSource, "Anime");
  private static String animeDestination = String.format(baseDestination,
      "Anime");

  private static String softwareSource = String.format(baseSource, "Software");
  private static String softwareDestination = String.format(baseDestination,
      "Software");

  private static final int WAIT_TIME = 10_000;

  public static void main(String[] args) {

    JSONObject parser = new JSONObject(System.getProperty("user.dir"
        + File.separator + "paths.json"));
    JSONArray paths = parser.getJSONArray("paths");

    for (int i = 0; i < paths.length(); i++) {
      JSONObject currentPath = paths.getJSONObject(i);
      String title = currentPath.getString("title");
      String source = currentPath.getString("source");
      String destination = currentPath.getString("destination");
      boolean placeInSub = currentPath.getBoolean("placeInSub");

      source = source.replaceAll("/", File.separator);
      destination = destination.replaceAll("/", File.separator);

      Watcher watcher = new Watcher(Paths.get(source), Paths.get(destination),
          placeInSub, WAIT_TIME, id++);
      threads.add(new Thread(watcher));

    }

    Watcher rssWatcher = new Watcher(Paths.get(rssSource), Paths.get
        (rssDestination), true,10_000, id++);
    threads.add(new Thread(rssWatcher));

    Watcher musicWatcher = new Watcher(Paths.get(musicSource),
        Paths.get(musicDestination), false,10_000, id++);
    threads.add(new Thread(musicWatcher));

    Watcher westernWatcher = new Watcher(Paths.get
        (westernSource), Paths.get(westernDestination), false,10_000, id++);
    threads.add(new Thread(westernWatcher));

    Watcher mangaWatcher = new Watcher(Paths.get(mangaSource),
        Paths.get(mangaDestination), false,10_000, id++);
    threads.add(new Thread(mangaWatcher));

    Watcher animeWatcher = new Watcher(Paths.get(animeSource),
        Paths.get(animeDestination), false,10_000, id++);
    threads.add(new Thread(animeWatcher));

    Watcher softwareWatcher = new Watcher(Paths.get(softwareSource),
        Paths.get(softwareDestination), false,10_000, id++);
    threads.add(new Thread(softwareWatcher));

    for (Thread thread : threads) {
      thread.start();
    }
  }

}

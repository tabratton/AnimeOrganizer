import com.dgtlrepublic.anitomyj.AnitomyJ;
import com.dgtlrepublic.anitomyj.Element;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Mover implements Runnable {

  private Path source;
  private Path destination;
  private Path filename;
  private int waitTime;
  private boolean fileMoved = false;
  private boolean alreadyTried = false;
  private boolean isDirectory;
  private boolean placeInSubFolder;
  private String title;
  private long id;

  public Mover(Path source, Path dest, Path filename, boolean sub, int wait,
               boolean isDirectory, long id) {
    this.source = source;
    this.destination = dest;
    this.filename = filename;
    this.placeInSubFolder = sub;
    this.waitTime = wait;
    this.isDirectory = isDirectory;
    this.title = getAnimeTitle(filename);
    this.id = id;
  }

  private String getAnimeTitle(Path path) {
    String filename = path.toString();
    List<Element> elements = AnitomyJ.parse(filename);
    String name = "";
    for (Element element : elements) {
      if (element.getCategory().name().equals("kElementAnimeTitle")) {
        name = element.getValue();
        break;
      }
    }

    return name;
  }

  public void run() {
    moveFile();
  }

  private void moveFile() {
    Path absoluteSrc = this.source.resolve(this.filename);
    Path absoluteDest;

    if (placeInSubFolder && !isDirectory) {
      createFolder(this.destination + File.separator + title);
      absoluteDest = Paths.get(this.destination + File.separator
          + this.title + File.separator + this.filename);
    } else {
      createFolder(this.destination.toString());
      absoluteDest = Paths.get(this.destination.toString()
          + File.separator + this.filename);
    }

    String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
    System.out.printf(Main.prefix, timeStamp, this.id, this.title, "Dest: "
        + absoluteDest +".%n");
    performMove(absoluteSrc, absoluteDest);
  }

  private void performMove(Path source, Path destination) {
    while (!this.fileMoved) {
      sleep();
      try {
        if (isDownloading(source.toFile())) {
          throw new IOException();
        }

        if (isDirectory) {
          FileUtils.copyDirectory(source.toFile(), destination.toFile());
        } else {
          Files.copy(source, destination, StandardCopyOption
              .REPLACE_EXISTING);
        }
      } catch (NoSuchFileException ex) {
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        System.err.printf(Main.prefix, timeStamp, this.id, this.title,
            filename + " has been deleted or cannot be found, stopping move"
                + " thread.%n");
        break;
      } catch (IOException ex) {
        if (this.alreadyTried) {
          continue;
        }
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        System.err.printf(Main.prefix, timeStamp, this.id, this.title,
            filename + " is currently being used by another process, waiting"
                + " until it is not being used to move.%n");
        this.alreadyTried = true;
        continue;
      }
      String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
      System.out.printf(Main.prefix, timeStamp, this.id, this.title,
          filename + " moved successfully.%n");
      fileMoved = true;
      Main.moved.add(this.filename.toString());
    }
  }

  private boolean isDownloading(File parent) {
    if (parent.isDirectory() && parent.listFiles() != null) {
      return Arrays.stream(parent.listFiles()).anyMatch(f -> f.toString()
          .contains(".lftp"));
    } else {
      String test = parent.toString() + ".lftp-pget-status";
      File testFile = new File(test);
      return testFile.exists();
    }
  }

  private void createFolder(String path) {
    File destinationFolder = new File(path);
    if (!destinationFolder.exists()) {
      destinationFolder.mkdir();
    }
  }

  private void sleep() {
    try {
      Thread.sleep(this.waitTime);
    } catch (InterruptedException exc) {
      System.err.println(exc.getMessage());
      String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
      System.err.printf(Main.prefix, timeStamp, this.id, this.title,
          "Thread stopped before moving file");
    }
  }
}

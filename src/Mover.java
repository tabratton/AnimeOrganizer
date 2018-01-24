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
import java.util.Arrays;
import java.util.List;

public class Mover implements Runnable {

  private Path source;
  private Path destination;
  private Path filename;
  private String folderID;
  private int waitTime;
  private boolean fileMoved = false;
  private boolean alreadyTried = false;
  private boolean isDirectory;
  private String title;
  private String threadId;

  public Mover(Path source, Path dest, Path filename, String
      folderID, int wait, boolean isDirectory, long id) {
    this.source = source;
    this.destination = dest;
    this.filename = filename;
    this.folderID = folderID;
    this.waitTime = wait;
    this.isDirectory = isDirectory;
    this.title = getAnimeTitle(filename);
    this.threadId = String.format("[ID %d]", id);
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
    if (folderID.equals("RSSFeed") && !isDirectory) {
      createFolder(this.destination + File.separator + title);
      absoluteDest = Paths.get(this.destination + File.separator
          + this.title + File.separator + this.filename);
    } else {
      createFolder(this.destination.toString());
      absoluteDest = Paths.get(this.destination.toString()
          + File.separator + this.filename);
    }

    System.out.printf("%s %s thread: Dest: %s%n", this.threadId, this.title,
        absoluteDest);
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
        System.err.printf("%s %s thread: %s has been deleted or cannot be"
                + " found, stopping move thread.%n", this.threadId, this.title,
            this.filename);
        break;
      } catch (IOException ex) {
        if (this.alreadyTried) {
          continue;
        }
        System.err.printf("%s %s thread: %s is currently being used by another"
                + " process, waiting until it is not being used to move.%n",
            this.threadId, this.title, this.filename);
        this.alreadyTried = true;
        continue;
      }
      System.out.printf("%s %s thread: %s moved successfully.%n", this.threadId,
          this.title, this.filename);
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
      System.err.printf("%s %s thread: Thread stopped before moving file.%n",
          this.threadId, this.title);
    }
  }
}

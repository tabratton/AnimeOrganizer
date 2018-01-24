import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class Watcher implements Runnable {

  private WatchService watcher;
  private Path source;
  private Path destination;
  private volatile boolean watching = false;
  private int waitTime;
  private String id;
  private String threadId;

  Watcher(String folder, Path source, Path dest, int wait, long id) {
    try {
      this.id = folder;
      this.watcher = FileSystems.getDefault().newWatchService();
      this.destination = dest;
      this.source = source;
      this.waitTime = wait;
      this.source.register(this.watcher, StandardWatchEventKinds.ENTRY_CREATE);
      this.threadId = String.format("[ID %d]", id);
      System.out.printf("Starting %s thread...%n", this.id);
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }
  }

  public void run() {
    startWatching();
  }

  private void startWatching() {
    this.watching = true;
    this.processEvents();
  }

  private void processEvents() {
    while (this.watching) {
      WatchKey key;

      // Wait for key to be signaled
      try {
        key = this.watcher.take();
      } catch (InterruptedException x) {
        return;
      }

      for (WatchEvent<?> event : key.pollEvents()) {
        WatchEvent.Kind kind = event.kind();

        if (kind == OVERFLOW) {
          continue;
        }

        // The filename is the context of the event.
        WatchEvent<Path> ev = (WatchEvent<Path>) event;
        Path filename = ev.context();

        if (Main.detected.contains(filename.toString())
            || filename.toString().contains(".lftp-pget-status")
            || filename.toString().contains("TeraCopy")) {
          continue;
        }

        // Determine whether the thing detected was a file or directory.
        Path child = this.source.resolve(filename);
        boolean isDirectory = Files.isDirectory(child);
        Main.detected.add(filename.toString());
        createMover(filename, isDirectory);
      }

      // Reset the key -- this step is critical if you want to receive
      // further watch events. If the key is no longer valid, the directory
      // is inaccessible so exit the loop.
      boolean valid = key.reset();
      if (!valid) {
        break;
      }
    }
  }

  private void createMover(Path filename, boolean isDirectory) {
    System.out.println();
    System.out.printf("%s %s thread: %s found, moving to correct folder.%n",
        this.threadId, this.id, filename);
    Mover mover = new Mover(this.source, this.destination, filename, this.id,
        this.waitTime, isDirectory, Main.id++);
    Thread thread = new Thread(mover);
    thread.start();
  }
}

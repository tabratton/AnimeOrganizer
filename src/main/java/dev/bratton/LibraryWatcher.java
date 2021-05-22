package dev.bratton;/*
 * Copyright (c) 2018, Tyler Bratton
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardWatchEventKinds.*;

public class LibraryWatcher implements Runnable {

  private WatchService watchService;
  private final String source;
  private final String destination;

  public LibraryWatcher(String source, String destination, String name) {
    this.source = source;
    this.destination = destination;

    var sourceList = recursiveScan(new File(source), source, new ArrayList<>());
    var destinationList = recursiveScan(new File(destination), destination, new ArrayList<>());

    destinationList.parallelStream()
        .filter(f -> !sourceList.contains(f))
        .forEach(f -> recursiveDelete(String.format("%s%s", this.destination, f.getName())));

    sourceList.parallelStream()
        .filter(f -> !destinationList.contains(f))
        .forEach(f -> copyFile(String.format("%s%s", this.source, f.getName())));

    try {
      this.watchService = FileSystems.getDefault().newWatchService();
      Files.walkFileTree(Paths.get(this.source), new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException ex) {
      System.out.println(ex.toString());
    }

    System.out.printf("Starting %s thread...%n", name);
  }

  @Override
  public void run() {
    try {
      this.processEvents();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  // TODO: debug this
  private void processEvents() throws InterruptedException {
    while (true) {
      var key = watchService.take();

      key.pollEvents().forEach(event -> {
        var kind = event.kind();
        var filename = event.context().toString();

        if (kind.equals(OVERFLOW) || filename.contains("TeraCopy")) {
          return;
        }

        handleFile(key, kind, filename);
      });

      if (!key.reset()) {
        break;
      }
    }
  }

  private void handleFile(WatchKey key, WatchEvent.Kind<?> kind, String filename) {
    var path = Paths.get(key.watchable().toString()).resolve(filename).toString();

    if (ENTRY_DELETE.equals(kind)) {
      recursiveDelete(path);
    }

    if (ENTRY_CREATE.equals(kind)) {
      copyFile(path);
    }
  }

  private void copyFile(String sourceName) {
    var destinationName = sourceName.replace(this.source, this.destination);
    var filenameParts = sourceName.split("\\\\");
    var filename = filenameParts[filenameParts.length - 1];
    Main.printStatusMessage(filename, String.format("Copying to %s", destinationName));
    new File(destinationName).getParentFile().mkdirs();
    try {
      Files.copy(Paths.get(sourceName), Paths.get(destinationName), StandardCopyOption.REPLACE_EXISTING);
      Main.printStatusMessage(filename, "Done copying");
    } catch (IOException ex) {
      System.out.println("There was a problem copying the file");
    }
  }

  private void recursiveDelete(String path) {
    var destPath = path.replace(this.source, this.destination);

    var filenameParts = path.split("\\\\");
    var filename = filenameParts[filenameParts.length - 1];
    Main.printStatusMessage(filename, String.format("Deleting %s", destPath));

    var file = new File(destPath);
    var parent = file.getParentFile();

    try {
      Files.delete(file.toPath());
    } catch (IOException ex) {
      System.out.printf("Error deleting file=%s; %s%n", file, ex);
    }

    var files = parent.listFiles();

    if (files != null && files.length == 0) {
      recursiveDelete(parent.getAbsolutePath());
    }

    Main.printStatusMessage(filename, "Done deleting");
  }

  private List<FileCompare> recursiveScan(File file, String prefix, List<FileCompare> list) {
    var arr = file.listFiles();

    if (arr == null) return list;

    for (var f : arr) {
      if (f.isDirectory()) {
        recursiveScan(f, prefix, list);
      } else {
        list.add(new FileCompare(f.getAbsolutePath().replace(prefix, ""), f.length()));
      }
    }

    return list;
  }

}

class FileCompare {
  private final String name;
  private final long size;

  public FileCompare(String name, long size) {
    this.name = name;
    this.size = size;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FileCompare that = (FileCompare) o;
    return size == that.size && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, size);
  }
}

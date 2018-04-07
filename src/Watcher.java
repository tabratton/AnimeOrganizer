/*
 * Copyright (c) 2018, Tyler Bratton
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class Watcher implements Runnable {

  private WatchService watcher;
  private Path source;
  private Path destination;
  private volatile boolean watching = false;
  private int waitTime;
  private boolean placeInSubFolder;
  private long id;
  private String name;

  Watcher(Path source, Path dest, boolean placeInSubFolder, int wait, long id) {

    this.placeInSubFolder = placeInSubFolder;
    this.destination = dest;
    this.source = source;
    this.waitTime = wait;
    this.id = id;
    String[] elements = source.toString().split(Main.regexChar);
    name = elements[elements.length - 1];

    try {
      this.watcher = FileSystems.getDefault().newWatchService();
      this.source.register(this.watcher, StandardWatchEventKinds.ENTRY_CREATE);
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }

    System.out.printf("Starting %s thread...%n", name);
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
    String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
    System.out.printf(Main.prefix, timeStamp, this.id, this.name,
        filename + " found, moving to correct folder.\n");
    Mover mover = new Mover(this.source, this.destination, filename,
        this.placeInSubFolder, this.waitTime, isDirectory, Main.id++);
    Thread thread = new Thread(mover);
    thread.start();
  }
}

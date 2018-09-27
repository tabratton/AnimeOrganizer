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

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class NormalWatcher implements Watcher {

  private WatchService watchService;
  private Path source;
  private Path destination;
  private int waitTime;
  private boolean placeInSubFolder;
  private String name;

  public NormalWatcher(Path source, Path dest, String name, boolean placeInSubFolder, int waitTime) {
    this.placeInSubFolder = placeInSubFolder;
    this.destination = dest;
    this.source = source;
    this.waitTime = waitTime;
    this.name = name;

    try {
      this.watchService = FileSystems.getDefault().newWatchService();
      this.source.register(this.watchService, StandardWatchEventKinds.ENTRY_CREATE);
    } catch (IOException ex) {
      System.err.println(ex.toString());
    }

    System.out.println(String.format("Starting %s thread...", name));
  }

  @Override
  public void run() {
    processEvents();
  }

  private void processEvents() {
    while (true) {
      WatchKey key;

      // Wait for key to be signaled
      try {
        key = this.watchService.take();
      } catch (InterruptedException x) {
      	Thread.currentThread().interrupt();
        return;
      }

      key.pollEvents().forEach(event -> {
	      var kind = event.kind();
	      String filename;
	      if (event.context() != null) {
		      filename = event.context().toString();
	      } else {
		      filename = null;
	      }

	      handleFile(kind, filename);
      });

      // Reset the key -- this step is critical if you want to receive
      // further watch events. If the key is no longer valid, the directory
      // is inaccessible so exit the loop.
      var valid = key.reset();
      if (!valid) {
        break;
      }
    }
  }

  private void handleFile(WatchEvent.Kind<?> kind, String filename) {
	  if (filename != null) {

		  if (kind.equals(OVERFLOW)
				  || Main.DETECTED.contains(filename)
				  || filename.contains(".lftp-pget-status")
				  || filename.contains("TeraCopy")) {
			  return;
		  }

		  // Determine whether the thing detected was a file or directory.
		  var child = this.source.resolve(filename);
		  var isDirectory = Files.isDirectory(child);
		  Main.DETECTED.add(filename);
		  createMover(filename, isDirectory);
	  }
  }

  private void createMover(String filename, boolean isDirectory) {
    Main.printStatusMessage(this.name, String.format("%s found, moving to correct folder", filename));
    var mover = new Mover(this.source, this.destination, filename, this.placeInSubFolder, this.waitTime, isDirectory);
    var thread = new Thread(mover);
    thread.start();
  }
}

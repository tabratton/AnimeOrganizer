package dev.bratton;/*
 * Copyright (c) 2018, Tyler Bratton
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Main {
  public static final String MESSAGE_FORMAT = "[%s] %s thread: %s%n";
  protected static final List<String> MOVED = new ArrayList<>();
  protected static final List<String> DETECTED = new ArrayList<>();

  private static final int WAIT_TIME = 5_000;

  public static void main(String[] args) throws IOException {
    String regexChar;
    if (File.separator.equals("/")) {
      regexChar = "/";
    } else {
      regexChar = "\\\\";
    }

    var threads = new LinkedList<Thread>();
    var sb = new StringBuilder();

    var directory = String.format("%s%spaths.json", System.getProperty("user.dir"), File.separator);
    try (var lines = Files.lines(Paths.get(directory))) {
      lines.forEach(sb::append);
    }

    var parser = new JSONObject(sb.toString());
    var paths = parser.getJSONArray("paths");

    for (var i = 0; i < paths.length(); i++) {
      var currentPath = paths.getJSONObject(i);
      var source = currentPath.getString("source").replaceAll("/", regexChar);
      var destination = currentPath.getString("destination").replaceAll("/", regexChar);
      var name = currentPath.getString("name");
      var placeInSub = currentPath.getBoolean("placeInSub");

      Runnable watcher;

      if (currentPath.getBoolean("library")) {
        watcher = new LibraryWatcher(source, destination, name);
      } else {
        watcher = new NormalWatcher(Paths.get(source), Paths.get(destination), name, placeInSub, WAIT_TIME);
      }

      threads.add(new Thread(watcher));
    }

    for (var thread : threads) {
      thread.start();
    }
  }

  public static void printStatusMessage(String name, String message) {
    var timeStamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    System.out.printf(Main.MESSAGE_FORMAT, timeStamp, name, message);
  }

}

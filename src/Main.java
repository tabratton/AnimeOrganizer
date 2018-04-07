/*
 * Copyright (c) 2018, Tyler Bratton
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Vector;

public class Main {
  public static ArrayList<String> moved = new ArrayList<>();
  public static ArrayList<String> detected = new ArrayList<>();
  public static long id = 1;
  public static String prefix = "[%s - ID %d] %s thread: %s";
  public static String regexChar;

  private static final int WAIT_TIME = 10_000;

  public static void main(String[] args) {
    if (File.separator.equals("/")) {
      regexChar = "/";
    } else {
      regexChar = "\\\\";
    }

    Vector<Thread> threads = new Vector<>();
    StringBuilder sb = new StringBuilder();

    try {
      String directory = System.getProperty("user.dir") + File.separator
          + "paths.json";
      Files.lines(Paths.get(directory)).forEach(sb::append);
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }

    JSONObject parser = new JSONObject(sb.toString());
    JSONArray paths = parser.getJSONArray("paths");

    for (int i = 0; i < paths.length(); i++) {
      JSONObject currentPath = paths.getJSONObject(i);
      String source = currentPath.getString("source");
      String destination = currentPath.getString("destination");
      boolean placeInSub = currentPath.getBoolean("placeInSub");

      source = source.replaceAll("/", regexChar);
      destination = destination.replaceAll("/", regexChar);

      Watcher watcher = new Watcher(Paths.get(source), Paths.get(destination),
          placeInSub, WAIT_TIME, id++);
      threads.add(new Thread(watcher));
    }

    for (Thread thread : threads) {
      thread.start();
    }
  }

}

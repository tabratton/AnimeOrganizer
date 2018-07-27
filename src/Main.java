/*
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
	protected static final String PREFIX = "[%s] %s thread: %s";
	protected static final List<String> moved = new ArrayList<>();
	protected static final List<String> detected = new ArrayList<>();

	private static final int WAIT_TIME = 5_000;

	public static void main(String[] args) {
		String regexChar;
		if (File.separator.equals("/")) {
			regexChar = "/";
		} else {
			regexChar = "\\\\";
		}

		List<Thread> threads = new ArrayList<>();
		var sb = new StringBuilder();
		var directory = System.getProperty("user.dir") + File.separator + "paths.json";

		try (var lines = Files.lines(Paths.get(directory))) {
			lines.forEach(sb::append);
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}

		var parser = new JSONObject(sb.toString());
		var paths = parser.getJSONArray("paths");

		for (int i = 0; i < paths.length(); i++) {
			var currentPath = paths.getJSONObject(i);
			var source = currentPath.getString("source").replaceAll("/", regexChar);
			var destination = currentPath.getString("destination").replaceAll("/", regexChar);
			var placeInSub = currentPath.getBoolean("placeInSub");
			var name = Arrays.stream(source.split(regexChar))
					.reduce((a, b) -> b)
					.orElse("");

			var watcher = new Watcher(Paths.get(source), Paths.get(destination), name, placeInSub, WAIT_TIME);
			threads.add(new Thread(watcher));
		}

		for (var thread : threads) {
			thread.start();
		}
	}

}

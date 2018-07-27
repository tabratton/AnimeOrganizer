/*
 * Copyright (c) 2018, Tyler Bratton
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import com.dgtlrepublic.anitomyj.AnitomyJ;
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

public class Mover implements Runnable {

	private static final String DATE_FORMAT = "yyyy.MM.dd.HH.mm.ss";
	private Path source;
	private Path destination;
	private Path filename;
	private int waitTime;
	private boolean fileMoved = false;
	private boolean alreadyTried = false;
	private boolean isDirectory;
	private boolean placeInSubFolder;
	private String title;

	public Mover(Path source, Path dest, Path filename, boolean sub, int wait, boolean isDirectory) {
		this.source = source;
		this.destination = dest;
		this.filename = filename;
		this.placeInSubFolder = sub;
		this.waitTime = wait;
		this.isDirectory = isDirectory;
		this.title = getAnimeTitle(filename);
	}

	private String getAnimeTitle(Path path) {
		var file = path.toString();
		var elements = AnitomyJ.parse(file);
		var name = elements.stream()
				.filter(e -> e.getCategory().name().equals("kElementAnimeTitle"))
				.findFirst();

		return name.isPresent() ? name.get().getValue() : "";
	}

	public void run() {
		var absoluteSrc = this.source.resolve(this.filename);
		var absoluteDest = setupDestinationFolder();
		performMove(absoluteSrc, absoluteDest);
	}

	private Path setupDestinationFolder() {
		Path absoluteDest;

		if (placeInSubFolder) {
			createFolder(this.destination + File.separator + title);
			absoluteDest = Paths.get(this.destination + File.separator + this.title + File.separator + this.filename);
		} else {
			createFolder(this.destination.toString());
			absoluteDest = Paths.get(this.destination.toString() + File.separator + this.filename);
		}

		var timeStamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
		System.out.printf(Main.PREFIX, timeStamp, this.title, "Dest: " + absoluteDest + ".\n");
		return absoluteDest;
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
					Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
				}

			} catch (NoSuchFileException ex) {
				var timeStamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
				System.err.printf(Main.PREFIX, timeStamp, this.title, filename + " has been deleted or cannot be found,"
						+ " stopping move thread.\n");
				return;
			} catch (IOException ex) {
				if (!this.alreadyTried) {
					var timeStamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
					System.err.printf(Main.PREFIX, timeStamp, this.title, filename + " is currently being used by another"
							+ " process, waiting until it is not being used to move.\n");
					this.alreadyTried = true;
				}

				continue;
			}

			fileMoved = true;
		}

		var timeStamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
		System.out.printf(Main.PREFIX, timeStamp, this.title, filename + " moved successfully.\n");
		Main.moved.add(this.filename.toString());
	}

	private boolean isDownloading(File current) {
		if (current.isDirectory() && current.listFiles() != null) {
			// Check each sub folder for downloading files as well.
			Arrays.stream(current.listFiles())
					.filter(File::isDirectory)
					.forEach(folder -> isDownloading(folder));

			return Arrays.stream(current.listFiles()).anyMatch(f -> f.toString().contains(".lftp"));
		} else {
			return new File(current.toString() + ".lftp-pget-status").exists();
		}
	}

	private void createFolder(String path) {
		var destinationFolder = new File(path);
		if (!destinationFolder.exists()) {
			try {
				var result = destinationFolder.mkdir();
				if (!result) {
					throw new IOException("Could not create folder");
				}
			} catch (IOException ex) {
				System.err.println(ex.getMessage());
				var timeStamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
				System.err.printf(Main.PREFIX, timeStamp, this.title, "Could not create folder, stopping thread\n");
				Thread.currentThread().interrupt();
			}
		}
	}

	private void sleep() {
		try {
			Thread.sleep(this.waitTime);
		} catch (InterruptedException exc) {
			System.err.println(exc.getMessage());
			var timeStamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
			System.err.printf(Main.PREFIX, timeStamp, this.title, "Thread stopped before moving file\n");
			Thread.currentThread().interrupt();
		}
	}
}

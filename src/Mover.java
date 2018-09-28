/*
 * Copyright (c) 2018, Tyler Bratton
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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

public class Mover implements Runnable {

	private Path source;
	private Path destination;
	private String filename;
	private int waitTime;
	private boolean fileMoved = false;
	private boolean alreadyTried = false;
	private boolean isDirectory;
	private boolean placeInSubFolder;
	private String title;

	public Mover(Path source, Path dest, String filename, boolean placeInSubFolder, int wait, boolean isDirectory) {
		this.source = source;
		this.destination = dest;
		this.filename = filename;
		this.placeInSubFolder = placeInSubFolder;
		this.waitTime = wait;
		this.isDirectory = isDirectory;
		this.title = getAnimeTitle(filename);
	}

	private String getAnimeTitle(String path) {
		return AnitomyJ.parse(path).stream()
				.filter(e -> e.getCategory().name().equals("kElementAnimeTitle"))
				.map(Element::getValue)
				.findAny()
				.orElse("");
	}

	public void run() {
		var absoluteSrc = this.source.resolve(this.filename);
		var absoluteDest = setupDestinationFolder();
		performMove(absoluteSrc, absoluteDest);
	}

	private Path setupDestinationFolder() {
		Path absoluteDest;

		if (this.placeInSubFolder) {
			var folder = this.destination + File.separator + this.title;
			createFolder(folder);
			absoluteDest = Paths.get(folder + File.separator + this.filename);
		} else {
			createFolder(this.destination.toString());
			absoluteDest = Paths.get(this.destination + File.separator + this.filename);
		}

		Main.printStatusMessage(this.title, String.format("Dest: %s", absoluteDest));
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
				Main.printStatusMessage(this.title, String.format("%s  has been deleted or cannot be found, stopping move"
						+ " thread", filename));
				return;
			} catch (IOException ex) {
				if (!this.alreadyTried) {
					Main.printStatusMessage(this.title, String.format("%s is currently being used by another process, waiting"
							+ " until it is not being used to move", filename));
					this.alreadyTried = true;
				}

				continue;
			}

			fileMoved = true;
		}

		Main.printStatusMessage(this.title, String.format("%s moved successfully", filename));
		Main.MOVED.add(this.filename);
	}

	private boolean isDownloading(File current) {
		if (current.isDirectory() && current.listFiles() != null) {
			return Arrays.stream(current.listFiles())
					.anyMatch(f -> f.toString().contains(".lftp"))
					|| Arrays.stream(current.listFiles())
					.filter(File::isDirectory)
					.map(this::isDownloading)
					.anyMatch(f -> true);
		} else {
			return new File(current.toString() + ".lftp-pget-status").exists();
		}
	}

	private void createFolder(String path) {
		var destinationFolder = new File(path);
		if (!destinationFolder.exists()) {
			try {
				if (!destinationFolder.mkdirs()) {
					throw new IOException("Could not create folders");
				}
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
				Main.printStatusMessage(this.title, "Could not create folder, stopping thread");
				Thread.currentThread().interrupt();
			}
		}
	}

	private void sleep() {
		try {
			Thread.sleep(this.waitTime);
		} catch (InterruptedException exc) {
			System.out.println(exc.getMessage());
			Main.printStatusMessage(this.title, "Thread stopped before moving file");
			Thread.currentThread().interrupt();
		}
	}
}

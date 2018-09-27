import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardWatchEventKinds.*;

public class LibraryWatcher implements Watcher {
	private static final int NUM_BYTES = 40960;

	private WatchService watchService;
	private String source;
	private String destination;
	private int time;
	private String name;

	public LibraryWatcher(String source, String destination, String name, int time) {
		this.source = source;
		this.destination = destination;
		this.time = time;
		this.name = name;

		var sourceList = recursiveScan(new File(source), source, new ArrayList<>());
		var destinationList = recursiveScan(new File(destination), destination, new ArrayList<>());

		destinationList.parallelStream()
				.filter(f -> !sourceList.contains(f))
				.forEach(f -> recursiveDelete(String.format("%s%s", this.destination, f.getName())));

		sourceList.parallelStream()
				.filter(f -> !destinationList.contains(f))
				.forEach(f -> {
					try {
						copyFile(String.format("%s%s", this.source, f.getName()));
					} catch (IOException ex) {
						System.out.println("There was a problem copying the file");
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
				});

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

		System.out.println(String.format("Starting %s thread...", this.name));
	}

	@Override
	public void run() {
		try {
			this.processEvents();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

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

		if (kind.equals(ENTRY_DELETE)) {
			recursiveDelete(path);
		}

		if (kind.equals(ENTRY_CREATE)) {
			try {
				copyFile(path);
			} catch (IOException ex) {
				System.out.println("There was a problem copying the file");
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void copyFile(String sourceName) throws IOException, InterruptedException {
		var destinationName = sourceName.replace(this.source, this.destination);
		var filenameParts = sourceName.split("\\\\");
		var filename = filenameParts[filenameParts.length - 1];
		Main.printStatusMessage(filename, String.format("Copying to %s", destinationName));

		new File(destinationName).getParentFile().mkdirs();
		var tried = false;
		while (true) {
			try (var input = new FileInputStream(sourceName); var output = new FileOutputStream(destinationName)) {
				var buffer = new byte[NUM_BYTES];
				var length = input.read(buffer);
				while (length > 0) {
					output.write(buffer, 0, length);
					length = input.read(buffer);
				}
				break;
			} catch (FileNotFoundException ex) {
				if (!tried) {
					Main.printStatusMessage(filename, "File is being used, waiting");
					tried = true;
				}
				Thread.sleep(this.time);
			}
		}

		Main.printStatusMessage(filename, "Done copying");
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
			System.out.println(String.format("Error deleting file=%s; %s", file.toString(), ex.toString()));
		}

		if (parent.listFiles() != null && parent.listFiles().length == 0) {
			recursiveDelete(parent.getAbsolutePath());
		}

		Main.printStatusMessage(filename, "Done deleting");
	}

	private List<FileCompare> recursiveScan(File file, String prefix, List<FileCompare> list) {
		var arr = file.listFiles();

		if (arr == null) return list;

		Arrays.stream(arr).forEach(f -> {
			if (f.isDirectory()) {
				recursiveScan(f, prefix, list);
			} else {
				list.add(new FileCompare(f.getAbsolutePath().replace(prefix, ""), f.length()));
			}
		});

		return list;
	}

}

class FileCompare {
	private String name;
	private long size;

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
		return size == that.size
				&& Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, size);
	}
}

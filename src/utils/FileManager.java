package utils;

import java.io.*;
import java.nio.file.*;


public class FileManager {

    private final String dataDirectory;

    public FileManager(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        createDirectoryIfAbsent();
    }

    public void save(String fileName, Object data) {
        String filePath = dataDirectory + File.separator + fileName;
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(filePath)))) {
            oos.writeObject(data);
        } catch (IOException e) {
            System.err.println("  [FileManager] Failed to save " + fileName + ": " + e.getMessage());
        }
    }

    public Object load(String fileName) {
        String filePath = dataDirectory + File.separator + fileName;
        File file = new File(filePath);
        if (!file.exists()) return null;

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(filePath)))) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("  [FileManager] Failed to load " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    public void exportToText(String fileName, String content) {
        String filePath = dataDirectory + File.separator + fileName;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(content);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("  [FileManager] Failed to export: " + e.getMessage());
        }
    }

    private void createDirectoryIfAbsent() {
        try {
            Files.createDirectories(Paths.get(dataDirectory));
        } catch (IOException e) {
            System.err.println("  [FileManager] Could not create data directory: " + e.getMessage());
        }
    }
}
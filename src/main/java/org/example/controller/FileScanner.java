package org.example.controller;

import java.io.File;
import javax.swing.DefaultListModel;

public class FileScanner {

    // Scans the root directories and populates the directory list model
    public static void scanRootDirectories(DefaultListModel<String> directoryListModel) {
        if (directoryListModel == null) {
            throw new IllegalArgumentException("directoryListModel cannot be null");
        }

        directoryListModel.clear(); // Clear the current list model

        // Get all root directories (e.g., C:\, D:\ on Windows)
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                // Add each root directory to the directory list model
                directoryListModel.addElement(root.getAbsolutePath());
            }
        }
    }

    // Scans the given directory and populates the file and directory list models
    public static void scanDirectory(File dir, DefaultListModel<String> fileListModel, DefaultListModel<String> directoryListModel) {
        if (fileListModel == null || directoryListModel == null) {
            throw new IllegalArgumentException("List models cannot be null");
        }

        fileListModel.clear(); // Clear the current file list model
        directoryListModel.clear(); // Clear the current directory list model

        // List all files and directories within the given directory
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Add directory name to the directory list model
                    directoryListModel.addElement(file.getAbsolutePath());
                } else {
                    // Add file name to the file list model
                    fileListModel.addElement(file.getAbsolutePath());
                }
            }
        }
    }
}

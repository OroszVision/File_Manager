package org.example;

import org.example.controller.FileScanner;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class FileExplorerFrame extends JFrame {
    private static final int ICON_SIZE = 24;
    private final DefaultListModel<String> fileListModel;
    private JTextField pathField;
    private JTree directoryTree;
    private DefaultTreeModel treeModel;
    private final DefaultListModel<String> directoryListModel;

    public FileExplorerFrame() {
        // Initialize models first
        fileListModel = new DefaultListModel<>();
        directoryListModel = new DefaultListModel<>();

        setupFrame();
        setupUIComponents();
        setupTreeListeners();
        scanRootDirectories();
    }

    private void setupFrame() {
        setTitle("File Explorer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 800);
        setLocationRelativeTo(null);
    }

    private void setupUIComponents() {
        pathField = new JTextField();

        directoryTree = setupDirectoryTree();
        JList<String> fileList = new JList<>(fileListModel);

        JButton scanButton = new JButton("Scan Directory");
        scanButton.addActionListener(e -> scanDirectory());

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(pathField, BorderLayout.CENTER);
        inputPanel.add(scanButton, BorderLayout.EAST);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(directoryTree), new JScrollPane(fileList));
        splitPane.setDividerLocation(200);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createTopPanel(), BorderLayout.NORTH);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JTree setupDirectoryTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Computer") {
            @Override
            public String toString() {
                return "Computer";
            }
        };
        treeModel = new DefaultTreeModel(root);
        JTree tree = new JTree(treeModel);

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node.getUserObject() instanceof File) {
                    File file = (File) node.getUserObject();
                    setText(file.getName().isEmpty() ? file.getAbsolutePath() : file.getName());
                } else {
                    setText(value.toString()); // For the "Computer" node or any non-File nodes
                }
                return this;
            }
        });

        tree.setShowsRootHandles(true);
        return tree;
    }



    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel topLeftPanel = new JPanel();
        JButton backButton = createButton("/icons/back-button.png", "Move Back in Directories", e -> moveBackInDirectories());
        topLeftPanel.add(backButton);

        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topRightPanel.add(createButton("/icons/delete.png", "Delete File", e -> deleteSelectedFile()));
        topRightPanel.add(createButton("/icons/open.png", "Open in File Explorer", e -> openFile()));
        topRightPanel.add(createButton("/icons/properties.png", "View Properties", e -> viewProperties()));
        topRightPanel.add(createButton("/icons/rename.png", "Rename File", e -> renameSelectedFile()));
        topRightPanel.add(createButton("/icons/file-explorer.png", "Open File Explorer", e -> openInFileExplorer()));

        topPanel.add(topLeftPanel, BorderLayout.WEST);
        topPanel.add(topRightPanel, BorderLayout.CENTER);
        return topPanel;
    }

    private JButton createButton(String iconPath, String toolTip, java.awt.event.ActionListener actionListener) {
        ImageIcon icon = resizeIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource(iconPath))));
        JButton button = new JButton(icon);
        button.setToolTipText(toolTip);
        button.addActionListener(actionListener);
        return button;
    }

    private ImageIcon resizeIcon(ImageIcon icon) {
        Image img = icon.getImage();
        Image resizedImg = img.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
        return new ImageIcon(resizedImg);
    }

    private void setupTreeListeners() {
        directoryTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) directoryTree.getLastSelectedPathComponent();
            if (selectedNode != null) {
                File selectedFile = (File) selectedNode.getUserObject();
                if (selectedFile.isDirectory()) {
                    pathField.setText(selectedFile.getAbsolutePath());
                    scanDirectoryAsync(selectedFile);
                }
            }
        });

        directoryTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                File dir = (File) node.getUserObject();
                if (dir.isDirectory()) {
                    expandDirectoryAsync(node, dir);
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                // Optionally handle collapsing events if needed
            }
        });
    }

    private void scanRootDirectories() {
        File[] roots = File.listRoots();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
        rootNode.removeAllChildren();

        for (File fileRoot : roots) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileRoot);
            rootNode.add(node);
            expandDirectoryAsync(node, fileRoot);
            directoryTree.expandPath(new TreePath(node.getPath()));
        }

        treeModel.reload();
    }


    private void expandDirectoryAsync(DefaultMutableTreeNode node, File dir) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                expandDirectory(node, dir);
                return null;
            }

            @Override
            protected void done() {
                treeModel.reload(node);
            }
        };
        worker.execute();
    }

    private void expandDirectory(DefaultMutableTreeNode node, File dir) {
        if (dir.isDirectory()) {
            node.removeAllChildren();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file);
                    node.add(childNode);
                    if (file.isDirectory()) {
                        childNode.add(new DefaultMutableTreeNode("Loading..."));
                    }
                }
            }
        }
    }



    private void scanDirectoryAsync(File dir) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                // Ensure directoryListModel is not null
                FileScanner.scanDirectory(dir, fileListModel, directoryListModel);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(FileExplorerFrame.this, "Error scanning directory: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void scanDirectory() {
        File dir = new File(pathField.getText());
        if (dir.isDirectory()) {
            scanDirectoryAsync(dir);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid directory path.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedFile() {
        String selectedFile = fileListModel.getElementAt(fileListModel.size() - 1); // Example retrieval
        if (selectedFile != null) {
            int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this file?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                File file = new File(selectedFile);
                if (file.delete()) {
                    fileListModel.removeElement(selectedFile);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete the file.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "No file selected for deletion.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void openInFileExplorer() {
        String selectedFile = fileListModel.getElementAt(fileListModel.size() - 1); // Example retrieval
        if (selectedFile != null) {
            try {
                Desktop.getDesktop().open(new File(selectedFile).getParentFile());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to open file explorer.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void moveBackInDirectories() {
        String currentPath = pathField.getText();
        File currentFile = new File(currentPath);
        File parentDir = currentFile.getParentFile();
        if (parentDir != null) {
            pathField.setText(parentDir.getAbsolutePath());
            scanDirectoryAsync(parentDir);
        }
    }

    private void openFile() {
        String selectedFile = fileListModel.getElementAt(fileListModel.size() - 1); // Example retrieval
        if (selectedFile != null) {
            try {
                Desktop.getDesktop().open(new File(selectedFile));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to open file.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void viewProperties() {
        String selectedFile = fileListModel.getElementAt(fileListModel.size() - 1); // Example retrieval
        if (selectedFile != null) {
            File file = new File(selectedFile);
            JOptionPane.showMessageDialog(this, String.format("Properties:\nName: %s\nSize: %d bytes\nPath: %s", file.getName(), file.length(), file.getAbsolutePath()), "File Properties", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void renameSelectedFile() {
        String selectedFile = fileListModel.getElementAt(fileListModel.size() - 1); // Example retrieval
        if (selectedFile != null) {
            String newName = JOptionPane.showInputDialog(this, "Enter new name:", "Rename File", JOptionPane.PLAIN_MESSAGE);
            if (newName != null) {
                File file = new File(selectedFile);
                File newFile = new File(file.getParent(), newName);
                if (file.renameTo(newFile)) {
                    fileListModel.set(fileListModel.indexOf(selectedFile), newFile.getAbsolutePath());
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to rename the file.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileExplorerFrame fileExplorerFrame = new FileExplorerFrame();
            fileExplorerFrame.setVisible(true);
        });
    }
}

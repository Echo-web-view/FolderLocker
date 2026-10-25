package com.folderlocker.platform;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WindowsShortcutManager {
    private static final String MARKER_FILE = "folder-locker.shortcut";

    public void ensureShortcut(Component parent, Path configFile) {
        if (!isWindows()) return;
        try {
            Path marker = configFile.toAbsolutePath().normalize().resolveSibling(MARKER_FILE);
            if (Files.exists(marker)) {
                String shortcut = Files.readString(marker, StandardCharsets.UTF_8).trim();
                if (!shortcut.isEmpty() && Files.isRegularFile(Path.of(shortcut))) return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("选择快捷方式保存位置");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;

            Path shortcut = chooser.getSelectedFile().toPath().resolve("FolderLocker.lnk");
            createShortcut(shortcut);
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, shortcut.toAbsolutePath().normalize().toString(), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(parent, "快捷方式已创建：\n" + shortcut,
                    "FolderLocker", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | URISyntaxException error) {
            JOptionPane.showMessageDialog(parent, "创建快捷方式失败：" + error.getMessage(),
                    "FolderLocker", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void createShortcut(Path shortcut) throws IOException, URISyntaxException {
        Path javaw = Path.of(System.getProperty("java.home"), "bin", "javaw.exe");
        if (!Files.isRegularFile(javaw)) throw new IOException("找不到 Java 启动程序：" + javaw);
        Path classPath = Path.of(WindowsShortcutManager.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();

        String script = "$shell = New-Object -ComObject WScript.Shell; "
                + "$shortcut = $shell.CreateShortcut('" + powershellQuote(shortcut.toString()) + "'); "
                + "$shortcut.TargetPath = '" + powershellQuote(javaw.toString()) + "'; "
                + "$shortcut.Arguments = '-cp \""
                + powershellQuote(classPath.toString()).replace("'", "''")
                + "\" com.folderlocker.Main'; "
                + "$shortcut.WorkingDirectory = '" + powershellQuote(workingDirectory.toString()) + "'; "
                + "$shortcut.Description = 'FolderLocker'; $shortcut.Save()";
        ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-Command", script);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().reduce("", (left, right) -> left + right + System.lineSeparator());
        }
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) throw new IOException(output.trim());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("创建快捷方式被中断", interrupted);
        }
    }

    private static String powershellQuote(String value) {
        return value.replace("'", "''");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}

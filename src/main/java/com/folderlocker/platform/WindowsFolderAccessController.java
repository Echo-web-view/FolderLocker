package com.folderlocker.platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WindowsFolderAccessController implements FolderAccessController {
    private static final String WINDOWS = "win";

    @Override
    public void lock(String folder) throws IOException {
        updateAcl(folder, "/deny", "*" + currentUser(), ":(OI)(CI)RX");
    }

    @Override
    public void unlock(String folder) throws IOException {
        updateAcl(folder, "/remove:d", "*" + currentUser(), null);
    }

    private void updateAcl(String folder, String operation, String user, String permissions) throws IOException {
        if (!System.getProperty("os.name").toLowerCase().contains(WINDOWS)) {
            throw new IOException("文件夹访问控制仅支持 Windows");
        }
        Path path = Path.of(folder).toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            throw new IOException("目录不存在或不是文件夹: " + path);
        }

        ProcessBuilder builder = new ProcessBuilder("icacls", path.toString(), operation,
                permissions == null ? user : user + permissions);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().reduce("", (left, right) -> left + right + System.lineSeparator());
        }
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("修改文件夹访问权限失败 (" + exitCode + "): " + output.trim());
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("修改文件夹访问权限被中断", interrupted);
        }
    }

    private static String currentUser() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("whoami", "/user", "/fo", "csv", "/nh");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().findFirst().orElse("");
        }
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("无法获取当前 Windows 用户标识");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("获取 Windows 用户标识被中断", interrupted);
        }
        String[] columns = output.split("\",\"");
        if (columns.length < 2) throw new IOException("无法解析当前 Windows 用户标识");
        return columns[1].replace("\"", "").trim();
    }
}

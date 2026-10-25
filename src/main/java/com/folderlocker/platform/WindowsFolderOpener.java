package com.folderlocker.platform;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WindowsFolderOpener {
    public void open(String folder) throws IOException {
        Path path = Path.of(folder).toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) throw new IOException("目录不存在或不是文件夹: " + path);
        if (!Desktop.isDesktopSupported()) throw new IOException("当前系统不支持打开文件夹");
        Desktop.getDesktop().open(path.toFile());
    }
}

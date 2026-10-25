package com.folderlocker.config;

import com.folderlocker.domain.FolderTarget;
import java.util.ArrayList;
import java.util.List;

public final class AppConfig {
    public static final int CURRENT_VERSION = 1;
    public static final int DEFAULT_WIDTH = 400;
    public static final int DEFAULT_HEIGHT = 300;

    private int version = CURRENT_VERSION;
    private int windowWidth = DEFAULT_WIDTH;
    private int windowHeight = DEFAULT_HEIGHT;
    private final List<FolderTarget> targets = new ArrayList<>();

    public int version() { return version; }
    public int windowWidth() { return windowWidth; }
    public int windowHeight() { return windowHeight; }
    public List<FolderTarget> targets() { return targets; }

    public void setWindowSize(int width, int height) {
        if (width < 300 || width > 1600 || height < 220 || height > 1200) {
            throw new IllegalArgumentException("window size is outside the supported range");
        }
        windowWidth = width;
        windowHeight = height;
    }
}

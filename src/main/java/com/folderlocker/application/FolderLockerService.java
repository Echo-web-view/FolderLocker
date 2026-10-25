package com.folderlocker.application;

import com.folderlocker.domain.FolderTarget;
import com.folderlocker.domain.LockState;
import com.folderlocker.platform.FolderAccessController;
import com.folderlocker.platform.WindowsFolderAccessController;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FolderLockerService {
    private final List<FolderTarget> targets;
    private final FolderAccessController accessController;

    public FolderLockerService(List<FolderTarget> targets) {
        this(targets, new WindowsFolderAccessController());
    }

    public FolderLockerService(List<FolderTarget> targets, FolderAccessController accessController) {
        this.targets = targets;
        this.accessController = accessController;
    }

    public boolean unlock(String targetId, String password) throws IOException {
        FolderTarget target = find(targetId);
        if (!target.password().equals(password)) return false;
        if (!Files.isDirectory(Path.of(target.path()))) return false;
        accessController.unlock(target.path());
        target.unlock();
        return true;
    }

    public void lock(String targetId) throws IOException {
        FolderTarget target = find(targetId);
        accessController.lock(target.path());
        target.lock();
    }

    public FolderTarget find(String targetId) {
        return targets.stream().filter(target -> target.id().equals(targetId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("target not found: " + targetId));
    }

    public boolean isUnlocked(String targetId) {
        return find(targetId).state() == LockState.UNLOCKED;
    }
}

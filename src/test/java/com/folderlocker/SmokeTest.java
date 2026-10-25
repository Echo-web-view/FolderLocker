package com.folderlocker;

import com.folderlocker.application.FolderLockerService;
import com.folderlocker.config.AppConfig;
import com.folderlocker.domain.FolderTarget;
import com.folderlocker.domain.AutoLockMode;
import com.folderlocker.persistence.JsonConfigRepository;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SmokeTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("folder-locker-test-");
        Path configFile = directory.resolve("folder-locker.json");
        AppConfig original = new AppConfig();
        original.setWindowSize(640, 480);
        String windowsLikePath = "\"" + directory.resolve("folder\\new") + "\"";
        original.targets().add(FolderTarget.create("测试 \\\"目录", windowsLikePath, "secret",
                AutoLockMode.LOCK_ON_APPLICATION_EXIT));

        JsonConfigRepository repository = new JsonConfigRepository();
        repository.save(configFile, original);
        AppConfig loaded = repository.load(configFile);
        check(loaded.windowWidth() == 640 && loaded.windowHeight() == 480, "窗口设置没有正确回读");
        check(loaded.targets().size() == 1, "目标数量没有正确回读");

        FolderTarget target = loaded.targets().get(0);
        check(target.path().equals(windowsLikePath.substring(1, windowsLikePath.length() - 1)),
                "带引号的 Windows 路径没有正确回读");
        check(target.autoLockMode() == AutoLockMode.LOCK_ON_APPLICATION_EXIT, "自动上锁选项没有正确回读");
        target.update(target.name(), directory.toString(), target.password());
        FolderLockerService service = new FolderLockerService(loaded.targets());
        check(!service.unlock(target.id(), "wrong"), "错误密码不应解锁");
        check(service.unlock(target.id(), "secret"), "正确密码应解锁");
        check(service.isUnlocked(target.id()), "解锁状态没有保存到内存");
        service.lock(target.id());
        check(!service.isUnlocked(target.id()), "锁定状态没有恢复");
        System.out.println("FolderLocker smoke test passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
package com.folderlocker.domain;

import java.util.Objects;
import java.util.UUID;

public final class FolderTarget {
    private final String id;
    private String name;
    private String path;
    private String password;
    private LockState state;
    private AutoLockMode autoLockMode;

    public FolderTarget(String id, String name, String path, String password, LockState state) {
        this(id, name, path, password, state, AutoLockMode.KEEP_UNLOCKED);
    }

    public FolderTarget(String id, String name, String path, String password, LockState state, AutoLockMode autoLockMode) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.path = normalizePath(path);
        this.password = Objects.requireNonNull(password, "password");
        this.state = Objects.requireNonNull(state, "state");
        this.autoLockMode = Objects.requireNonNull(autoLockMode, "autoLockMode");
    }

    public static FolderTarget create(String name, String path, String password) {
        return new FolderTarget(UUID.randomUUID().toString(), name, path, password, LockState.LOCKED);
    }

    public static FolderTarget create(String name, String path, String password, AutoLockMode autoLockMode) {
        return new FolderTarget(UUID.randomUUID().toString(), name, path, password, LockState.LOCKED, autoLockMode);
    }

    public String id() { return id; }
    public String name() { return name; }
    public String path() { return path; }
    public String password() { return password; }
    public LockState state() { return state; }
    public AutoLockMode autoLockMode() { return autoLockMode; }

    public void update(String name, String path, String password) {
        this.name = requireText(name, "name");
        this.path = normalizePath(path);
        this.password = Objects.requireNonNull(password, "password");
    }

    public void unlock() { state = LockState.UNLOCKED; }
    public void lock() { state = LockState.LOCKED; }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return value.trim();
    }

    private static String normalizePath(String value) {
        String path = requireText(value, "path");
        if (path.length() >= 2 && path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1).trim();
        }
        return requireText(path, "path");
    }
}

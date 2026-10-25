package com.folderlocker.persistence;

import com.folderlocker.config.AppConfig;
import com.folderlocker.domain.FolderTarget;
import com.folderlocker.domain.AutoLockMode;
import com.folderlocker.domain.LockState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonConfigRepository {
    private static final Pattern NUMBER = Pattern.compile("\\\"%s\\\"\\s*:\\s*(\\d+)");
    private static final Pattern TARGET = Pattern.compile("\\{\\s*\\\"id\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"\\s*,\\s*\\\"name\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"\\s*,\\s*\\\"path\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"\\s*,\\s*\\\"password\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"\\s*,\\s*\\\"state\\\"\\s*:\\s*\\\"(LOCKED|UNLOCKED)\\\"(?:\\s*,\\s*\\\"autoLock\\\"\\s*:\\s*\\\"(KEEP_UNLOCKED|LOCK_ON_APPLICATION_EXIT)\\\")?\\s*\\}");

    public AppConfig load(Path file) throws IOException {
        if (!Files.exists(file)) return new AppConfig();
        String json = Files.readString(file, StandardCharsets.UTF_8);
        AppConfig config = new AppConfig();
        config.setWindowSize(number(json, "windowWidth", AppConfig.DEFAULT_WIDTH), number(json, "windowHeight", AppConfig.DEFAULT_HEIGHT));
        Matcher matcher = TARGET.matcher(json);
        while (matcher.find()) {
            config.targets().add(new FolderTarget(unescape(matcher.group(1)), unescape(matcher.group(2)),
                    unescape(matcher.group(3)), unescape(matcher.group(4)), LockState.valueOf(matcher.group(5)),
                    matcher.group(6) == null ? AutoLockMode.KEEP_UNLOCKED : AutoLockMode.valueOf(matcher.group(6))));
        }
        return config;
    }

    public void save(Path file, AppConfig config) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        StringBuilder json = new StringBuilder("{\n  \"version\": 1,\n  \"settings\": {\n    \"windowWidth\": ").append(config.windowWidth())
            .append(",\n    \"windowHeight\": ").append(config.windowHeight()).append("\n  },\n  \"targets\": [\n");
        for (int i = 0; i < config.targets().size(); i++) {
            FolderTarget target = config.targets().get(i);
                json.append("    {\n      \"id\": ").append(quote(target.id())).append(",\n      \"name\": ").append(quote(target.name()))
                    .append(",\n      \"path\": ").append(quote(target.path())).append(",\n      \"password\": ").append(quote(target.password()))
                    .append(",\n      \"state\": ").append(quote(target.state().name()))
                    .append(",\n      \"autoLock\": ").append(quote(target.autoLockMode().name())).append("\n    }");
            if (i + 1 < config.targets().size()) json.append(',');
            json.append('\n');
        }
        json.append("  ]\n}\n");
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, json, StandardCharsets.UTF_8);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static int number(String json, String key, int fallback) {
        Matcher matcher = Pattern.compile(String.format(NUMBER.pattern(), key)).matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(character);
            }
        }
        return escaped.append('"').toString();
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character != '\\' || i + 1 >= value.length()) {
                result.append(character);
                continue;
            }
            char escaped = value.charAt(++i);
            switch (escaped) {
                case '\\', '"', '/' -> result.append(escaped);
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                default -> result.append('\\').append(escaped);
            }
        }
        return result.toString();
    }
}

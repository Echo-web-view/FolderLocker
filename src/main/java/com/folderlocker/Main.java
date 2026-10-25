package com.folderlocker;

import com.folderlocker.application.FolderLockerService;
import com.folderlocker.config.AppConfig;
import com.folderlocker.domain.FolderTarget;
import com.folderlocker.persistence.JsonConfigRepository;
import com.folderlocker.platform.WindowsFolderOpener;
import com.folderlocker.platform.WindowsShortcutManager;
import com.folderlocker.ui.PasswordDialog;
import com.folderlocker.ui.TargetEditorDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;

public final class Main {
    private static final Path CONFIG_FILE = Path.of("config", "folder-locker.json");
    private final AppConfig config;
    private final JsonConfigRepository repository = new JsonConfigRepository();
    private final FolderLockerService service;
    private final DefaultListModel<FolderTarget> listModel = new DefaultListModel<>();
    private final JList<FolderTarget> targetList = new JList<>(listModel);
    private final JLabel status = new JLabel("请选择一个文件夹目标");

    private Main(AppConfig config) {
        this.config = config;
        this.service = new FolderLockerService(config.targets());
        config.targets().forEach(target -> {
            if (target.state() == com.folderlocker.domain.LockState.LOCKED) {
                try {
                    service.lock(target.id());
                } catch (IOException error) {
                    status.setText("无法应用锁定权限：" + error.getMessage());
                }
            }
            listModel.addElement(target);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                AppConfig config = new JsonConfigRepository().load(CONFIG_FILE);
                new Main(config).show();
            } catch (Exception error) {
                String message = "读取配置失败：" + error.getMessage();
                if (GraphicsEnvironment.isHeadless()) System.err.println(message);
                else JOptionPane.showMessageDialog(null, message, "FolderLocker", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void show() {
        JFrame frame = new JFrame("FolderLocker");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                lockTargetsOnApplicationExit();
            }
        });
        frame.setSize(config.windowWidth(), config.windowHeight());
        frame.setMinimumSize(new Dimension(400, 300));
        frame.setLocationByPlatform(true);
        new WindowsShortcutManager().ensureShortcut(frame, CONFIG_FILE);
        targetList.setCellRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel((selected ? "  " : "") + value.name() + " - " + value.state());
            label.setOpaque(true);
            label.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            label.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
            return label;
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("添加");
        JButton unlock = new JButton("解锁/打开");
        JButton lock = new JButton("锁定");
        JButton remove = new JButton("删除");
        buttons.add(add); buttons.add(unlock); buttons.add(lock); buttons.add(remove);
        add.addActionListener(event -> addTarget(frame));
        unlock.addActionListener(event -> unlockTarget(frame));
        lock.addActionListener(event -> lockTarget());
        remove.addActionListener(event -> removeTarget());
        frame.add(buttons, BorderLayout.NORTH);
        frame.add(new JScrollPane(targetList), BorderLayout.CENTER);
        frame.add(status, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void addTarget(Component parent) {
        try {
            FolderTarget target = TargetEditorDialog.create(parent);
            if (target != null) {
                config.targets().add(target);
                try {
                    service.lock(target.id());
                } catch (IOException error) {
                    config.targets().remove(target);
                    throw error;
                }
                listModel.addElement(target);
                save();
            }
        } catch (Exception error) { showError(parent, error); }
    }

    private void unlockTarget(Component parent) {
        FolderTarget target = targetList.getSelectedValue();
        if (target == null) { status.setText("请先选择目标"); return; }
        String password = PasswordDialog.show(parent, target.name(), config.windowWidth(), config.windowHeight());
        if (password == null) return;
        try {
            if (!service.unlock(target.id(), password)) { status.setText("密码错误或目录不可用"); return; }
        } catch (IOException error) {
            status.setText("解锁失败：" + error.getMessage());
            return;
        }
        targetList.repaint(); save();
        try { new WindowsFolderOpener().open(target.path()); status.setText("已解锁并打开：" + target.path()); }
        catch (Exception error) { status.setText(error.getMessage()); }
    }

    private void lockTarget() {
        FolderTarget target = targetList.getSelectedValue();
        if (target == null) return;
        try {
            service.lock(target.id());
            targetList.repaint();
            save();
            status.setText("已锁定：" + target.name());
        } catch (Exception error) {
            status.setText("锁定失败：" + error.getMessage());
        }
    }
    private void lockTargetsOnApplicationExit() {
        for (FolderTarget target : config.targets()) {
            if (target.autoLockMode() != com.folderlocker.domain.AutoLockMode.LOCK_ON_APPLICATION_EXIT
                    || target.state() == com.folderlocker.domain.LockState.LOCKED) continue;
            try {
                service.lock(target.id());
            } catch (IOException error) {
                System.err.println("退出时无法锁定目录 " + target.path() + "：" + error.getMessage());
            }
        }
        save();
    }
    private void removeTarget() { FolderTarget target = targetList.getSelectedValue(); if (target != null) { config.targets().remove(target); listModel.removeElement(target); save(); status.setText("已删除：" + target.name()); } }
    private void save() { try { repository.save(CONFIG_FILE, config); } catch (Exception error) { status.setText("保存配置失败：" + error.getMessage()); } }
    private void showError(Component parent, Exception error) { JOptionPane.showMessageDialog(parent, error.getMessage(), "FolderLocker", JOptionPane.ERROR_MESSAGE); }
}

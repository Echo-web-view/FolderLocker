package com.folderlocker.ui;

import com.folderlocker.domain.FolderTarget;
import com.folderlocker.domain.AutoLockMode;
import javax.swing.*;
import java.awt.*;

public final class TargetEditorDialog {
    private TargetEditorDialog() { }

    public static FolderTarget create(Component parent) {
        JTextField name = new JTextField();
        JTextField path = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<AutoLockMode> autoLock = new JComboBox<>(AutoLockMode.values());
        autoLock.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean isSelected, boolean cellHasFocus) {
                String text = value == AutoLockMode.LOCK_ON_APPLICATION_EXIT
                        ? "程序退出时自动上锁" : "保持解锁";
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
        JPanel panel = form(name, path, password, autoLock);
        int result = JOptionPane.showConfirmDialog(parent, panel, "添加文件夹", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;
        return FolderTarget.create(name.getText(), path.getText(), new String(password.getPassword()),
                (AutoLockMode) autoLock.getSelectedItem());
    }

    private static JPanel form(JTextField name, JTextField path, JPasswordField password,
                               JComboBox<AutoLockMode> autoLock) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(new JLabel("名称")); panel.add(name);
        panel.add(new JLabel("文件夹路径")); panel.add(path);
        panel.add(new JLabel("密码")); panel.add(password);
        panel.add(new JLabel("自动上锁")); panel.add(autoLock);
        return panel;
    }
}

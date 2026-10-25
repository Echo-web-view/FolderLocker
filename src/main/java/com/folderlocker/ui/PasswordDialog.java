package com.folderlocker.ui;

import javax.swing.*;
import java.awt.*;

public final class PasswordDialog {
    private PasswordDialog() { }

    public static String show(Component parent, String targetName, int width, int height) {
        JPasswordField password = new JPasswordField();
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        panel.add(new JLabel("请输入“" + targetName + "”的密码："), BorderLayout.NORTH);
        panel.add(password, BorderLayout.CENTER);
        JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = optionPane.createDialog(parent, "FolderLocker - 解锁");
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        if (optionPane.getValue() instanceof Integer value && value == JOptionPane.OK_OPTION) {
            return new String(password.getPassword());
        }
        return null;
    }
}

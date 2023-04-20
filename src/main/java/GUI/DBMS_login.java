package GUI;

import com.formdev.flatlaf.FlatDarkLaf;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class DBMS_login {
    private JPanel rootPanel;
    private JTextField usernameText;
    private JPasswordField passwordText;
    private JButton loginButton;

    public DBMS_login() {
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //获取用户名和密码
                String username = usernameText.getText();
                String password = new String(passwordText.getPassword());

                //是否登录成功
                boolean flag = false;
                //记录用户名
                String userName="";

                SAXReader reader = new SAXReader();
                File file = new File("src/main/resources/LoginList/userList.xml");
                try {
                    Document document = reader.read(file);
                    Element rootElement = document.getRootElement();
                    List<Element> users = rootElement.elements();
                    for (Element user : users) {
                        if (user.attributeValue("username").equals(username) && user.attributeValue("password").equals(password)) {
                            flag = true;
                            userName=user.attributeValue("username");
                            break;
                        }

                    }

                } catch (DocumentException ex) {
                    throw new RuntimeException(ex);
                }


                if (flag) {
                    JOptionPane.showMessageDialog(null, "登录成功", "用户登录", JOptionPane.INFORMATION_MESSAGE, null);
                    DBMS_main.createWindow(userName);
                    //退出登录界面
                    JComponent comp = (JComponent) e.getSource();
                    Window win = SwingUtilities.getWindowAncestor(comp);
                    win.dispose();
                } else {
                    JOptionPane.showMessageDialog(null, "登录失败,用户名或者密码有误", "登录失败", JOptionPane.ERROR_MESSAGE, null);
                }
                return;
            }
        });
    }

    public static void main(String[] args) {

        FlatDarkLaf.setup();
        JFrame frame = new JFrame("数据库管理系统");
        frame.setPreferredSize(new Dimension(400, 300));//设置窗口大小
        frame.setLocation(760, 390);
        frame.setContentPane(new DBMS_login().rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

}

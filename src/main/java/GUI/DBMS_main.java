package GUI;

import SQL.*;
import com.formdev.flatlaf.FlatDarkLaf;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DBMS_main {
    private String userName;//用户名
    private JPanel rootPanel;
    private JTextArea InputTextArea;
    private JButton SubmitButton;
    private JButton ClearButton;
    public JTextArea OutputTextArea;

    public void setOutputTextArea(String str){
        this.OutputTextArea.setText(str);
        return;
    }

    public DBMS_main(String username) {
        this.userName=username;//获取用户名

        SubmitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //获取并处理sql语句
                String InputString=InputTextArea.getText();
                String sql=InputString.replaceAll("\r|\n|\t","");
                sql=sql.trim();
                sql=sql.toLowerCase();

                //清空InputTextArea和OutputTextArea区域内的文字
                InputTextArea.setText("");
                OutputTextArea.setText("");

                //获取sql语句的首位关键字
                String[] str=sql.split(" ");
                String flag=str[0];

                //返回的执行结果
                String result;

                switch(flag){
                    case "create":
                        if(str[1].equals("user")){
                            //创建用户
                            result= CreateUser.createUser(sql);
                            OutputTextArea.setText("SQL语句 :"+sql+'\n'+result);
                        }else if(str[1].equals("table")){
                            //创建表
                            result= CreateTable.createTable(sql,userName);
                            OutputTextArea.setText("SQL语句 :"+sql+'\n'+result);
                        }else{
                            OutputTextArea.setText("SQL语句 :"+sql+'\n'+"SQL语句语法不正确");
                        }
                        break;
                    case "select":
                        result=Select.select(sql,userName);
                        OutputTextArea.setText("SQL语句 :"+sql+'\n'+result);
                        break;
                    case "insert":
                        //插入数据
                        result=Insert.insert(sql,userName);
                        OutputTextArea.setText("SQL语句 :"+sql+'\n'+result);
                        break;
                    case "alter":
                        result=Alter.alter(sql,userName);
                        OutputTextArea.setText("SQL语句 :"+sql+'\n'+result);
                        break;
                    case "update":
                        result=Update.update(sql,userName);
                        OutputTextArea.setText("SQL语句 :"+sql+'\n'+result);
                        break;
                    case "delete":
                        result=Delete.delete(sql,userName);
                        OutputTextArea.setText("SQL语句 :"+sql+'\n'+result);
                        break;
                    case "drop":
                        if(str[1].equals("user")){
                            //删除用户
                            result= DropUser.dropUser(sql,userName);
                            OutputTextArea.setText("SQL语句 :"+sql+'\n'+result);
                        }else if(str[1].equals("table")){
                            //删除表
                            result=DropTable.dropTable(sql,userName);
                            OutputTextArea.setText("SQL语句 :"+sql+'\n'+result);
                        }else{
                            OutputTextArea.setText("SQL语句 :"+sql+'\n'+"SQL语句语法不正确");
                        }
                        break;
                    case "grant":
                        result=Grant.grant(sql);
                        OutputTextArea.setText("SQL语句 :"+sql+'\n'+result);
                        break;
                    case "revoke":
                        result= Revoke.revoke(sql);
                        OutputTextArea.setText("SQL语句 :"+sql+'\n'+result);
                        break;
                    default:OutputTextArea.setText("SQL语句出现错误,无法编译");
                }

                return;
            }
        });

        //清空按钮
        ClearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InputTextArea.setText("");
                return;
            }
        });
    }

    @Test
    public static void createWindow(String username) {

        FlatDarkLaf.setup();
        JFrame frame = new JFrame("DBMS");
        frame.setPreferredSize(new Dimension(800, 600));//设置窗口大小
        frame.setLocation(580, 240);//居中显示
        frame.setContentPane(new DBMS_main(username).rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        return;
    }

}

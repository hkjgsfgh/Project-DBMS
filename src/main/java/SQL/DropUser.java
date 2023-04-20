package SQL;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DropUser {
    public static String dropUser(String sql,String username){
        try{

            //drop user 'user01'@'localhost';
            String regStr = "^\\s*drop\\s+user\\s+'(?<userName>\\w+)'@'(?<host>.+)'\\s*;?$";
            Pattern pattern = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE); //不区分大小写
            Matcher matcher = pattern.matcher(sql);

            if(!matcher.find()){
                return "SQL语句语法不正确";
            }

            String userName=matcher.group("userName");
            String host=matcher.group("host");

            //return userName+'\n'+username+'\n';//测试

            Pattern pattern2 = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
            Matcher matcher2 = pattern2.matcher(host);
            if (!(host.toLowerCase().equals("localhost") || host.equals("%") || matcher2.find())) {
                return "host的名称不正确";
            }

            //判断删除的是否是当前登录的用户名
            if(userName.equals(username)){
                return "无法删除当前正在登录的用户";
            }

            SAXReader reader = new SAXReader();
            File file = new File("src/main/resources/LoginList/userList.xml");
            Document document = reader.read(file);

            boolean flag=false;//是否查找到相应用户
            Element rootElement = document.getRootElement();
            List<Element> users = rootElement.elements();
            for(Element user : users){
                if(userName.equals(user.attribute("username").getValue())){
                    flag=true;//找到该用户
                    rootElement.remove(user);//通过父节点删除该用户
                }
            }

            if(!flag){
                return "用户名不存在";
            }

            //写入XML文件
            OutputFormat format= OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            OutputStream out=new FileOutputStream("src/main/resources/LoginList/userList.xml");
            XMLWriter writer=new XMLWriter(out,format);
            writer.write(document);
            writer.close();

        }catch(Exception e){
            return "编译出现错误";
        }

        return "SQL语句执行成功";

    }
}

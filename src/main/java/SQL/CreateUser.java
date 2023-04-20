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

public class CreateUser {
    public static String createUser(String sql){

        try{

            //create user 'user01' @'localhost' identified by 'user01';
            String regStr = "^\\s*create\\s+user\\s+'(?<userName>\\w+)'@'(?<host>.+)'\\s+identified\\s+by\\s+'(?<password>\\w+)'\\s*;?$";
            Pattern pattern = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE); //不区分大小写
            Matcher matcher = pattern.matcher(sql);

            if(!matcher.find()){
                return "SQL语句语法不正确";
            }

            String userName=matcher.group("userName");
            String password=matcher.group("password");
            String host=matcher.group("host");

            Pattern pattern2 = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
            Matcher matcher2 = pattern2.matcher(host);
            if (!(host.toLowerCase().equals("localhost") || host.equals("%") || matcher2.find())) {
                return "host的名称不正确";
            }

            SAXReader reader = new SAXReader();
            File file = new File("src/main/resources/LoginList/userList.xml");
            Document document = reader.read(file);


            Element rootElement = document.getRootElement();

            //判断用户名是否重复
            List<Element> users = rootElement.elements();
            for(Element user:users){
                if(userName.equals(user.attributeValue("username"))){
                    return "用户名已存在";
                }
            }

            //为用户添加属性
            Element newuser = rootElement.addElement("user");
            newuser.addAttribute("username",userName);
            newuser.addAttribute("password",password);
            newuser.addAttribute("create","true");
            newuser.addAttribute("all","");
            newuser.addAttribute("select","");
            newuser.addAttribute("insert","");
            newuser.addAttribute("update","");
            newuser.addAttribute("delete","");
            newuser.addAttribute("alter","");
            newuser.addAttribute("drop","");

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

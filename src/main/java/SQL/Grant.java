package SQL;

import GUI.DBMS_main;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Grant {
    public Grant() throws DocumentException {
    }

    public static String grant(String sql) {
        //用户授权
        String regStr = "^\\s*grant\\s+(?<privileges>\\w+)\\s+on\\s+(?<dbName>\\w+)\\.(?<tableName>\\w+)\\s+to\\s+'(?<userName>\\w+)'@'(?<host>.+)'\\s*;?$";
        Pattern pattern = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE); //不区分大小写
        Matcher matcher = pattern.matcher(sql);
        //GRANT privileges ON databasename.tablename TO 'username'@'host'

        //未匹配到则返回错误
        if (!matcher.find()) {
            return "SQL语句语法不正确";
        }

        //System.out.println(matcher.group("privileges")+'\n'+matcher.group("dbName")+'\n'+matcher.group("tableName")+'\n'+matcher.group("userName")+'\n'+matcher.group("host"));
        String privileges = matcher.group("privileges");
        String tableName = matcher.group("tableName");
        String host = matcher.group("host");
        String userName = matcher.group("userName");
        String dbName = matcher.group("dbName");

        //判断数据库名称是否正确
        if(!dbName.equals("Database")){
            return "数据库名称不正确";
        }

        //判断host名称是否正确
        Pattern pattern2 = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
        Matcher matcher2 = pattern2.matcher(host);
        if (!(host.toLowerCase().equals("localhost") || host.equals("%") || matcher2.find())) {
            return "host的名称不正确";
        }

        try {
            File file = new File("src/main/resources/Database/" + tableName + ".xml");
            if (!file.exists()) {
                return "查询不到表名";
            }
        } catch (Exception e) {
            return "查询不到表名";
        }

        try {
            //根据XML文档创建DOM4J树
            SAXReader reader = new SAXReader();
            File file = new File("src/main/resources/LoginList/userList.xml");
            Document document = reader.read(file);

            boolean username_flag=false;
            //获取用户列表的根节点
            Element rootElement = document.getRootElement();
            List<Element> users = rootElement.elements();
            for (Element user : users) {
                if (userName.equals(user.attributeValue("username"))) {  //用户名区分大小写
                    username_flag=true; //找到该用户
                    if (user.attributeValue("all").contains(tableName)) {
                        return "语句执行成功";
                    }

                    switch (privileges.toLowerCase()) {
                        case "all":
                            user.attribute("create").setValue("true");
                            List<Attribute> attributes = user.attributes();
                            for (int i = 3; i < attributes.size(); i++) {
                                if(!attributes.get(i).getValue().contains(tableName)){
                                    attributes.get(i).setValue(attributes.get(i).getValue()+tableName + ',');
                                }
                            }
                            break;
                        case "select":
                            if(!user.attributeValue("select").contains(tableName)){
                                user.attribute("select").setValue(user.attributeValue("select")+tableName + ',');
                            }
                            break;
                        case "insert":
                            if(!user.attributeValue("insert").contains(tableName)){
                                user.attribute("insert").setValue(user.attributeValue("insert")+tableName + ',');
                            }
                            break;
                        case "update":
                            if(!user.attributeValue("update").contains(tableName)){
                                user.attribute("update").setValue(user.attributeValue("update")+tableName + ',');
                            }
                            break;
                        case "delete":
                            if(!user.attributeValue("delete").contains(tableName)){
                                user.attribute("delete").setValue(user.attributeValue("delete")+tableName + ',');
                            }
                            break;
                        case "alter":
                            if(!user.attributeValue("alter").contains(tableName)){
                                user.attribute("alter").setValue(user.attributeValue("alter")+tableName + ',');
                            }
                            break;
                        case "drop":
                            if(!user.attributeValue("drop").contains(tableName)){
                                user.attribute("drop").setValue(user.attributeValue("drop")+tableName + ',');
                            }
                            break;
                        default:
                            return "权限名称不正确";
                    }
                }
            }
            if(!username_flag){
                return "未查询到该用户名";
            }

            //写入XML文件
            OutputFormat format= OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            OutputStream out=new FileOutputStream("src/main/resources/LoginList/userList.xml");
            XMLWriter writer=new XMLWriter(out,format);
            writer.write(document);
            writer.close();

            return "语句执行成功";
        } catch (Exception e) {
            return "编译出现错误";
        }

    }
}

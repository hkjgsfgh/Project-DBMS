package SQL;

import GUI.DBMS_main;
import org.dom4j.Attribute;
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

public class Revoke {
    public static String revoke(String sql){
        //撤销用户授权
        String regStr = "^\\s*revoke\\s+((?<privileges>\\w+)|(?<all>all\\s+privileges))\\s+on\\s+(?<dbName>\\w+)\\.(?<tableName>\\w+)\\s+from\\s+'(?<userName>\\w+)'@'(?<host>.+)'\\s*?;$";
        Pattern pattern = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE); //不区分大小写
        Matcher matcher = pattern.matcher(sql);
        //REVOKE INSERT, UPDATE, DELETE ON database_name.* FROM 'user_name'@'host';

        //未匹配到则返回错误
        if (!matcher.find()) {
            return "SQL语句语法不正确";
        }

        //测试
        //return matcher.group("privileges")+'\n'+matcher.group("all")+'\n'+matcher.group("dbName")+'\n'+matcher.group("tableName")+'\n'+matcher.group("userName")+'\n'+matcher.group("host");
        String privileges = matcher.group("privileges");
        String all = matcher.group("all");
        String tableName = matcher.group("tableName");
        String host = matcher.group("host");
        String userName = matcher.group("userName");
        String dbName = matcher.group("dbName");

        //判断数据库名称是否正确
        if(!dbName.equals("Database")){
            return "数据库名称不正确";
        }

        //检测host格式是否正确
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
                if (userName.equals(user.attributeValue("username"))) {
                    username_flag=true; //找到该用户

                    //将all属性清空
                    user.attribute("all").setValue(user.attributeValue("all").replaceAll(tableName+",",""));

                    //如果是撤销所有权限
                    if(all!=null){
                        user.attribute("create").setValue("false");
                        List<Attribute> attributes = user.attributes();
                        for(int i=4;i<attributes.size();i++){
                            String str = attributes.get(i).getValue();
                            str=str.replaceAll(tableName+",","");
                            attributes.get(i).setValue(str);
                        }
                        break;
                    }

                    switch (privileges.toLowerCase()) {
                        case "create":
                            user.attribute("create").setValue("false");
                            break;
                        case "select":
                            user.attribute("select").setValue(user.attributeValue("select").replaceAll(tableName+",",""));
                            break;
                        case "insert":
                            user.attribute("insert").setValue(user.attributeValue("insert").replaceAll(tableName+",",""));
                            break;
                        case "update":
                            user.attribute("update").setValue(user.attributeValue("update").replaceAll(tableName+",",""));
                            break;
                        case "delete":
                            user.attribute("delete").setValue(user.attributeValue("delete").replaceAll(tableName+",",""));
                            break;
                        case "alter":
                            user.attribute("alter").setValue(user.attributeValue("alter").replaceAll(tableName+",",""));
                            break;
                        case "drop":
                            user.attribute("drop").setValue(user.attributeValue("drop").replaceAll(tableName+",",""));
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

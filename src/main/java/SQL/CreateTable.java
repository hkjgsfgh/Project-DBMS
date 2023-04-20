package SQL;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateTable {
    public static String createTable(String sql,String userName){
        try{
            String regStr="^\\s*create\\s+table\\s+(?<tableName>\\w+)\\s*\\((?<attributes>.+)\\)\\s*;?$";
            Pattern pattern = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE); //不区分大小写
            Matcher matcher = pattern.matcher(sql);

            if(!matcher.find()){
                return "SQL语句语法不正确";
            }

            String tableName=matcher.group("tableName");
            String attributes=matcher.group("attributes");

            //先查看用户是否具有权限
            SAXReader reader_1 = new SAXReader();
            File file = new File("src/main/resources/LoginList/userList.xml");
            Document document_1 = reader_1.read(file);

            Element rootElement_1 = document_1.getRootElement();
            List<Element> users = rootElement_1.elements();
            for(Element user:users){
                if(userName.equals(user.attributeValue("username"))){
                    if(user.attributeValue("create").equals("false")){
                        return "用户不具有该表的create权限";
                    }

                }
            }

            //如果具有权限
            //再判断是否存在该表名
            File new_file=new File("src/main/resources/Database/"+tableName.toLowerCase()+".xml");
            if(new_file.exists()){
                return "该表名已经存在,无法创建";
            }


            //创建DOM文档
            Document document= DocumentHelper.createDocument();
            document.addComment("表名 :"+tableName);
            //添加根节点
            Element rootElement = document.addElement(tableName + "s");

            //添加键元素
            Element keys = rootElement.addElement("keys");
            //添加主键元素 :唯一
            keys.addAttribute("primary_key","");
            keys.addAttribute("isRelated","false");


            //FOREIGN KEY (a_id) REFERENCES Vendors (b_id)
            //PRIMARY KEY (id)

            Pattern pattern1 = Pattern.compile("^\\s*primary\\s+key\\((?<primaryKey>\\w+)\\)\\s*$", Pattern.CASE_INSENSITIVE); //不区分大小写
            Pattern pattern2 = Pattern.compile("^\\s*foreign\\s+key\\((?<foreignKey>\\w+)\\)\\s+references\\s+(?<tableName>\\w+)\\((?<primaryKey>\\w+)\\)\\s*$", Pattern.CASE_INSENSITIVE);
            Pattern pattern3=  Pattern.compile("^\\s*(?<value>\\w+)\\s+(?<attribute>(int|float|double|date|varchar\\(\\w+\\)|char\\(\\w+\\)))\\s*$",Pattern.CASE_INSENSITIVE);

            String PK="";
            Element table = rootElement.addElement("attributes");//表的字段节点

            //判断属性字段是否符合规定 :
            String[] attributeList=attributes.split(",");
            for(String attribute : attributeList){
                String[] str = attribute.split(" ");
                //判断是否为主键
                if(str[0].toLowerCase().equals("primary")){
                    if(!PK.equals("")){
                        return "不能添加多个主键";
                    }

                    Matcher matcher1=pattern1.matcher(attribute);
                    //是否匹配
                    if(!matcher1.find()){
                        return "SQL语句语法不正确";
                    }
                    //如果匹配
                    PK=matcher1.group("primaryKey");
                    keys.attribute("primary_key").setValue(PK);

                }
                //判断是否为外键
                else if(str[0].toLowerCase().equals("foreign")){

                    Matcher matcher2=pattern2.matcher(attribute);
                    if(!matcher2.find()){
                        return "SQL语句语法不正确";
                    }

                    String FK=matcher2.group("foreignKey");//外键
                    String PK_tableName=matcher2.group("tableName");//主表
                    String out_PK=matcher2.group("primaryKey");//主键

                    Element FKey = keys.addElement("FKey");
                    FKey.addAttribute("FK",FK);
                    FKey.addAttribute("PK",out_PK);
                    FKey.addAttribute("tableName",PK_tableName);

                    //为主表添加外键约束
                    addFKConstraint(PK_tableName);
                }else{
                    Matcher matcher3=pattern3.matcher(attribute);
                    if(!matcher3.find()){
                        return "SQL语句语法不正确";
                    }

                    String value=matcher3.group("value");
                    String attribute_type=matcher3.group("attribute");

                    table.addAttribute(value,attribute_type);
                }

            }

            //创建xml文件
            if(!new_file.createNewFile()){
                return "创建表时发生错误";
            }

            //写入XML文件
            OutputFormat format= OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            OutputStream out=new FileOutputStream("src/main/resources/Database/"+tableName.toLowerCase()+".xml");
            XMLWriter writer=new XMLWriter(out,format);
            writer.write(document);
            writer.close();

        }catch(Exception e){
            return "编译出现错误";
        }

        return "SQL语句执行成功";

    }

    //为主表添加外键约束
    public static void addFKConstraint(String tableName) throws DocumentException, IOException {
        SAXReader reader = new SAXReader();
        File file = new File("src/main/resources/Database/"+tableName.toLowerCase()+".xml");
        Document document = reader.read(file);

        Element rootElement = document.getRootElement();
        Element keys = rootElement.element("keys");
        keys.attribute("isRelated").setValue("true");

        //写入XML文件
        OutputFormat format= OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        OutputStream out=new FileOutputStream("src/main/resources/Database/"+tableName.toLowerCase()+".xml");
        XMLWriter writer=new XMLWriter(out,format);
        writer.write(document);
        writer.close();

        return;
    }
}

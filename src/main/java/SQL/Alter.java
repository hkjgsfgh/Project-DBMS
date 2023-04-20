package SQL;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Alter {
    public static String alter(String sql,String userName){
        try{
            //ALTER TABLE  student DROP sno;
            //ALTER TABLE student ADD i INT;

            String regStr="^\\s*alter\\s+table\\s+(?<tableName>\\w+)\\s+(drop|add)\\s+((?<attributeDrop>\\w+)|(?<attributeAdd>\\w+)\\s+(?<type>[\\w\\(\\)]+))\\s*;?$";
            Pattern pattern = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE); //不区分大小写
            Matcher matcher = pattern.matcher(sql);

            if(!matcher.find()){
                return "SQL语句语法不正确";
            }

            String operator=matcher.group(2);
            String tableName=matcher.group("tableName");
            String attributeDrop=matcher.group("attributeDrop");
            String type=matcher.group("type");
            String attributeAdd=matcher.group("attributeAdd");

            //测试return tableName+"\n"+operator+'\n'+attributeDrop+'\n'+attributeAdd+"\n"+value+'\n';

            //判断是否有权限
            SAXReader reader1 = new SAXReader();
            Document doc = reader1.read("src/main/resources/LoginList/userList.xml");
            List<Element> users = doc.getRootElement().elements();
            for(Element user : users){
                if(userName.equals(user.attributeValue("username"))){

                    String[] s=user.attributeValue("alter").split(",");
                    boolean res = Arrays.asList(s).contains(tableName);
                    if(!res){
                        return "用户不具有该表的alter权限";
                    }
                }

            }

            //判断表是否存在
            File file=new File("src/main/resources/Database/"+tableName.toLowerCase()+".xml");
            if(!file.exists()){
                return "该表名不存在,无法添加字段";
            }

            SAXReader reader = new SAXReader();
            Document document = reader.read(file);
            Element rootElement = document.getRootElement();

            //如果具有权限,判断操作符
            if(operator.toLowerCase().equals("drop")){  //删除字段操作
                //判断删除的字段是否为主键
                if(attributeDrop.toLowerCase().equals(rootElement.element("keys").attributeValue("primary_key"))){
                    return "删除的字段是主键,无法删除";
                }

                Element attr = rootElement.element("attributes");
                Attribute attribute = attr.attribute(attributeDrop);
                attr.remove(attribute);

                //删除表中的值
                List<Element> elementList = rootElement.elements();
                for(int i=2;i<elementList.size();i++){
                    Attribute attribute_drop = elementList.get(i).attribute(attributeDrop);
                    elementList.get(i).remove(attribute_drop);
                }

            }else{ //添加字段
                List<Element> elementList = rootElement.elements();
                Element attributtes = rootElement.element("attributes");
                attributtes.addAttribute(attributeAdd,type);

                for(int i=2;i<elementList.size();i++){
                    elementList.get(i).addAttribute(attributeAdd,"");
                }
            }

            //写入XML文件
            OutputFormat format= OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            OutputStream out=new FileOutputStream("src/main/resources/Database/"+tableName.toLowerCase()+".xml");
            XMLWriter writer=new XMLWriter(out,format);
            writer.write(document);
            writer.close();

            return "SQL语句执行成功";

        }catch(Exception e){
            return "编译出现错误";
        }

    }
}

package SQL;

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

public class DropTable {
    public static String dropTable(String sql,String username){

        try{

            //drop table student;
            String regStr = "^\\s*drop\\s+table\\s+(?<tableName>\\w+);?$";
            Pattern pattern = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE); //不区分大小写
            Matcher matcher = pattern.matcher(sql);

            if(!matcher.find()){
                return "SQL语句语法不正确";
            }

            String tableName=matcher.group("tableName");

            //判断是否有权限
            SAXReader reader1 = new SAXReader();
            Document doc = reader1.read("src/main/resources/LoginList/userList.xml");
            List<Element> users = doc.getRootElement().elements();
            for(Element user : users){
                if(username.equals(user.attributeValue("username"))){

                    String[] s=user.attributeValue("drop").split(",");
                    boolean res = Arrays.asList(s).contains(tableName);
                    if(!res){
                        return "用户不具有该表的create权限";
                    }
                }

            }

            //若具有权限
            //判断表是否存在
            File file=new File("src/main/resources/Database/"+tableName.toLowerCase()+".xml");
            if(!file.exists()){
                return "该表名不存在,无法删除";
            }

            //若存在,则判断是否存在外键约束
            SAXReader reader = new SAXReader();
            Document document = reader.read(file);
            Element rootElement = document.getRootElement();
            if(rootElement.element("keys").attributeValue("isRelated").equals("true")){
                return "该表存在外键约束,无法删除该表";
            }

            //若不存在外键约束,则删除
            file.delete();

        }catch(Exception e){
            return "编译出现错误";
        }

        return "SQL语句执行成功";

    }
}

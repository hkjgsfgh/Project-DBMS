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

public class Delete {
    public static String delete(String sql,String userName){

        try{
            //DELETE FROM runoob_tbl WHERE runoob_id=3;
            String regStr="^\\s*delete\\s+from\\s+(?<tableName>\\w+)\\s*(where\\s+(?<condition>.+))?\\s*;$";
            Pattern pattern = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE); //不区分大小写
            Matcher matcher = pattern.matcher(sql);

            if(!matcher.find()){
                return "SQL语句语法不正确";
            }

            String tableName=matcher.group("tableName");
            String condition=matcher.group("condition");

            //测试return tableName+"\n"+condition;

            //判断是否有权限
            SAXReader reader1 = new SAXReader();
            Document doc = reader1.read("src/main/resources/LoginList/userList.xml");
            List<Element> users = doc.getRootElement().elements();
            for(Element user : users){
                if(userName.equals(user.attributeValue("username"))){

                    String[] s=user.attributeValue("delete").split(",");
                    boolean res = Arrays.asList(s).contains(tableName);
                    if(!res){
                        return "用户不具有该表的delete权限";
                    }
                }

            }

            //判断表是否存在
            File file=new File("src/main/resources/Database/"+tableName.toLowerCase()+".xml");
            if(!file.exists()){
                return "该表名不存在,无法删除元组";
            }



            SAXReader reader = new SAXReader();
            Document document = reader.read(file);
            Element rootElement = document.getRootElement();

            //判断是否存在外键约束
            if(rootElement.element("keys").attributeValue("isRelated").equals("true")){
                return "该表存在外键约束,无法删除该表";
            }

            Pattern pattern1 = Pattern.compile("^\\s*(?<attribute>\\w+)\\s*=(?<value>.+)\\s*", Pattern.CASE_INSENSITIVE); //不区分大小写

            List<Element> elementList = rootElement.elements();
            //判断是否存在条件
            if(condition==null){ //不存在条件,删除全表
                //List<Element> elementList = rootElement.elements();
                for(int i=2;i<elementList.size();i++){
                    rootElement.remove(elementList.get(i));
                }
            }else{ //条件删除
                //判断是否包含and或者or
                if(condition.toLowerCase().contains("and")){

                    //and条件
                    String[] conditions=condition.split("and");
                    int len=elementList.size();
                    for(int i=2;i<elementList.size();i++){
                        int j=0;
                        for(String con:conditions){
                            Matcher matcher1=pattern1.matcher(con);
                            if(!matcher1.find()){
                                return "SQL语句语法不正确";
                            }
                            String attribute=matcher1.group("attribute");
                            attribute=attribute.trim();
                            String value=matcher1.group("value");
                            value=value.trim();

                            if(elementList.get(i).attributeValue(attribute).equals(value)){
                                j++;
                            }else{
                                break;
                            }
                        }

                        if(j== conditions.length){
                            rootElement.remove(elementList.get(i));
                        }
                    }
                    if(elementList.size()==len){
                        return "删除的条件查找不到相应的列,无法删除";
                    }

                }else if(condition.toLowerCase().contains("or")){
                    //or条件
                    String[] conditions=condition.split("or");

                    //List<Element> elementList = rootElement.elements();
                    for(String con:conditions){
                        Matcher matcher1=pattern1.matcher(con);
                        if(!matcher1.find()){
                            return "SQL语句语法不正确";
                        }

                        String attribute=matcher1.group("attribute");
                        attribute=attribute.trim();
                        String value=matcher1.group("value");
                        value=value.trim();
                        //System.out.println(attribute+"\n"+value);

                        for(int i=2;i<elementList.size();i++){
                            if(elementList.get(i).attributeValue(attribute).equals(value)){
                                rootElement.remove(elementList.get(i));
                            }
                        }
                    }
                }else{  //单条件
                    Matcher matcher1=pattern1.matcher(condition);
                    if(!matcher1.find()){
                        return "SQL语句语法不正确";
                    }
                    String attribute=matcher1.group("attribute");
                    attribute=attribute.trim();
                    String value=matcher1.group("value");
                    value=value.trim();

                    for(int i=2;i<elementList.size();i++){
                        if(elementList.get(i).attributeValue(attribute).equals(value)){
                            rootElement.remove(elementList.get(i));
                        }
                    }
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

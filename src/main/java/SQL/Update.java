package SQL;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Update {
    public static String update(String sql,String userName){
        try{
            //update table_name set name='vox' where id=1 and sex='f';
            String regStr="^\\s*update\\s+(?<tableName>\\w+)\\s+set\\s+(?<attributeSet>\\w+)\\s*=(?<valueSet>[\\w'']+)(\\s+where\\s+(?<condition>.+))?\\s*;$";
            Pattern pattern = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE); //不区分大小写
            Matcher matcher = pattern.matcher(sql);

            if(!matcher.find()){
                return "SQL语句语法不正确";
            }

            String tableName=matcher.group("tableName");
            tableName=tableName.trim();
            String attribute_set = matcher.group("attributeSet");
            attribute_set=attribute_set.trim();
            String value_set = matcher.group("valueSet");
            value_set=value_set.trim();
            String condition=matcher.group("condition");


            //测试return tableName+"\n"+attribute_set+"\n"+value_set+"\n"+condition;

            //判断是否有权限
            SAXReader reader1 = new SAXReader();
            Document doc = reader1.read("src/main/resources/LoginList/userList.xml");
            List<Element> users = doc.getRootElement().elements();
            for(Element user : users){
                if(userName.equals(user.attributeValue("username"))){

                    String[] s=user.attributeValue("update").split(",");
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

            List<Element> elementList = rootElement.elements();

            Pattern pattern1 = Pattern.compile("^\\s*(?<attribute>\\w+)\\s*=(?<value>.+)\\s*", Pattern.CASE_INSENSITIVE);

            //判断是否存在条件
            if(condition==null){ //不存在条件,修改全表
                for(int i=2;i<elementList.size();i++){
                    elementList.get(i).attribute(attribute_set).setValue(value_set);
                }
            }else{ //条件删除
                //判断是否包含and或者or
                if(condition.toLowerCase().contains("and")){

                    //and条件
                    String[] conditions=condition.split("and");
                    int k=0;
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
                            k++;
                            elementList.get(i).attribute(attribute_set).setValue(value_set);
                        }
                    }
                    if(k==0){
                        return "修改的条件查找不到相应的列,无法修改";
                    }

                }else if(condition.toLowerCase().contains("or")){
                    //or条件
                    String[] conditions=condition.split("or");

                    for(String con:conditions){
                        Matcher matcher1=pattern1.matcher(con);
                        if(!matcher1.find()){
                            return "SQL语句语法不正确";
                        }

                        String attribute=matcher1.group("attribute");
                        attribute=attribute.trim();
                        String value=matcher1.group("value");
                        value=value.trim();

                        for(int i=2;i<elementList.size();i++){
                            if(elementList.get(i).attributeValue(attribute).equals(value)){
                                elementList.get(i).attribute(attribute_set).setValue(value_set);
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
                            elementList.get(i).attribute(attribute_set).setValue(value_set);
                        }
                    }
                }
            }

            //判断主键约束
            String PK = rootElement.element("keys").attributeValue("primary_key");
            if(PK!=""){//具有主键约束
                List<String> PK_list = new ArrayList<>();
                for(int i=2;i<elementList.size();i++){
                    //如果不存在，则添加到列表中
                    if (!PK_list.contains(elementList.get(i).attributeValue(PK))){
                        PK_list.add(elementList.get(i).attributeValue(PK));
                    }else {
                        //如果存在主键的值重复，则返回错误
                        return "主键值不能重复,无法完成数据修改操作";
                    }

                }
            }

            //判断外键约束,使用containsAll()方法
            List<Element> FKeys = rootElement.element("keys").elements();

            if(FKeys!=null){//具有外键约束
                for(Element fkey:FKeys){
                    String fk=fkey.attributeValue("FK");
                    String pk=fkey.attributeValue("PK");
                    String tbname=fkey.attributeValue("tableName");

                    List<String> fk_list=new ArrayList<String>();
                    List<String> pk_list=new ArrayList<String>();

                    //为外键列表赋值
                    for(int i=2;i<elementList.size();i++){
                        if (!fk_list.contains(elementList.get(i).attributeValue(fk))){
                            fk_list.add(elementList.get(i).attributeValue(fk));
                        }
                    }

                    //为主键列表赋值
                    SAXReader reader_2 = new SAXReader();
                    File file_2 = new File("src/main/resources/Database/"+tbname+".xml");
                    Document document_2 = reader_2.read(file_2);

                    Element rootElement_2 = document_2.getRootElement();
                    List<Element> elementList_2 = rootElement_2.elements();
                    for(int i=2;i<elementList_2.size();i++){
                        if (!pk_list.contains(elementList_2.get(i).attributeValue(pk))){
                            pk_list.add(elementList_2.get(i).attributeValue(pk));
                        }
                    }

                    //判断pk_list是否包含fk_list
                    if(!pk_list.containsAll(fk_list)){
                        return "插入的值不符合外键约束,无法插入数据";
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

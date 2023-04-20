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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Insert {
    public static String insert(String sql,String username){

        try{
            //INSERT INTO table_name ( field1, field2,...fieldN ) VALUES ( value1, value2,...valueN );
            String regStr="^\\s*insert\\s+into\\s+(?<tableName>\\w+)\\s*(\\((?<attributes>.+)\\))?\\s+values\\s*\\((?<values>.+)\\)\\s*;\\s*$";
            Pattern pattern = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE); //不区分大小写
            Matcher matcher = pattern.matcher(sql);

            if(!matcher.find()){
                return "SQL语句语法不正确";
            }

            String tableName=matcher.group("tableName");
            String attributes=matcher.group("attributes");
            String values=matcher.group("values");

            //测试
            //return tableName+"\n"+attributes+"\n"+values;

            //查看用户是否具有权限
            SAXReader reader_1 = new SAXReader();
            File file_1 = new File("src/main/resources/LoginList/userList.xml");
            Document document_1 = reader_1.read(file_1);

            Element rootElement_1 = document_1.getRootElement();
            List<Element> users = rootElement_1.elements();
            for(Element user:users){
                if(username.equals(user.attributeValue("username"))){
                    String[] s=user.attributeValue("insert").split(",");
                    boolean res = Arrays.asList(s).contains(tableName);
                    if(!res){
                        return "用户不具有该表的insert权限";
                    }
                }
            }

            //若用户具有权限
            //判断表是否存在
            File file=new File("src/main/resources/Database/"+tableName.toLowerCase()+".xml");
            if(!file.exists()){
                return "该表名不存在,无法插入数据";
            }

            //若表存在
            SAXReader reader = new SAXReader();
            Document document = reader.read(file);
            Element rootElement = document.getRootElement();



            //插入数值
            if(attributes!=null){ //若传入字段名
                String[] attr = attributes.split(",");
                String[] value = values.split(",");

                if(attr.length!=value.length){
                    return "传入的字段值数量与字段数量不同,无法执行SQL语句";
                }
                Element table = rootElement.addElement(tableName);
                //插入数值
                for(int i=0;i<attr.length;i++){
                    table.addAttribute(attr[i],value[i]);
                }

            }else{//若未传入字段名

                String[] value = values.split(",");
                List<Attribute> attrs = rootElement.element("attributes").attributes();

                System.out.println(value.length+"\n"+attrs.size());
                if(attrs.size()!=value.length){
                    return "传入的字段值数量不等于属性值,无法执行SQL语句";
                }
                Element table = rootElement.addElement(tableName);
                for(int i=0;i<value.length;i++){
                    table.addAttribute(attrs.get(i).getName(),value[i]);
                }
            }

            //判断主键约束
            String PK = rootElement.element("keys").attributeValue("primary_key");
            if(PK!=""){//具有主键约束
                List<Element> elements = rootElement.elements();
                List<String> PK_list = new ArrayList<>();
                for(int i=2;i<elements.size();i++){
                    //如果不存在，则添加到列表中
                    if (!PK_list.contains(elements.get(i).attributeValue(PK))){
                        PK_list.add(elements.get(i).attributeValue(PK));
                    }else {
                        //如果存在主键的值重复，则返回错误
                        return "主键值不能重复,无法完成数据插入操作";
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
                    List<Element> elementList = rootElement.elements();
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


        }catch(Exception e){
            return "编译出现错误";
        }

        return "SQL语句执行成功";

    }
}

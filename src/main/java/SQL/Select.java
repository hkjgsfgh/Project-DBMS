package SQL;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Select {
    //排序使用TreeMap实现
    public static String select(String sql,String userName){
        try{
            //select a.id,b.name from tableA inner join tableB on a.id=b.id
            //where name='vox'
            //order by id desc;
            String regStr="^\\s*select\\s+(?<selects>[\\w\\.,]+)\\s+from\\s+(?<table1>\\w+)\\s*(inner\\s+join\\s+(?<table2>\\w+)\\s+on\\s+(?<joincondition>.*?))?\\s*(where\\s+(?<conditions>.*?))?(\\s*order\\s+by\\s+(?<orderAttribute>[\\w\\.]+)\\s+(?<order>(asc|desc)))?\\s*;$";
            Pattern pattern = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE); //不区分大小写
            Matcher matcher = pattern.matcher(sql);

            if(!matcher.find()){
                return "SQL语句语法不正确";
            }

            String selects=matcher.group("selects");
            selects=selects.trim();
            String tableName1=matcher.group("table1");
            tableName1=tableName1.trim();
            String tableName2=matcher.group("table2");
            String joinCondition=matcher.group("joincondition");
            String conditions=matcher.group("conditions");
            String orderAttribute=matcher.group("orderAttribute");
            String order=matcher.group("order");

            //测试return selects+"\n"+tableName1+"\n"+tableName2+"\n"+joinCondition+"\n"+conditions+"\n"+orderAttribute+"\n"+order;

            //判断是否有权限
            SAXReader reader1 = new SAXReader();
            Document doc = reader1.read("src/main/resources/LoginList/userList.xml");
            List<Element> users = doc.getRootElement().elements();
            for(Element user : users){
                if(userName.equals(user.attributeValue("username"))){

                    String[] s=user.attributeValue("select").split(",");
                    boolean res = Arrays.asList(s).contains(tableName1);
                    if(!res){
                        return "用户不具有该表的select权限";
                    }
                }

            }

            //判断表是否存在
            File file=new File("src/main/resources/Database/"+tableName1.toLowerCase()+".xml");
            if(!file.exists()){
                return "该表名不存在,无法查询";
            }

            SAXReader reader = new SAXReader();
            Document document = reader.read(file);
            Element rootElement = document.getRootElement();

            List<Element> elementList = rootElement.elements();
            String result="";//select的查询结果

            String format="%-15s";//定义对齐格式

            Pattern pattern1 = Pattern.compile("^\\s*(?<attribute>[\\w\\.]+)\\s*=(?<value>[\\w'']+)\\s*", Pattern.CASE_INSENSITIVE);
            Pattern pattern2 = Pattern.compile("^\\s*(?<tableName1>\\w+)\\s*\\.\\s*(?<attribute1>\\w+)=(?<tableName2>\\w+)\\s*\\.\\s*(?<attribute2>\\w+)\\s*", Pattern.CASE_INSENSITIVE);
            Pattern pattern3 = Pattern.compile("^\\s*(?<tableName>\\w+)\\s*\\.\\s*(?<attribute>\\w+)=\\s*(?<value>.+)\\s*", Pattern.CASE_INSENSITIVE);

            //判断是否为联表查询
            if(tableName2==null){
                //添加属性列到result中
                String[] select = selects.split(",");
                for(String s:select){
                    s=s.trim();
                    result=result+String.format(format,s);
                }
                result=result+"\n";

                //判断是否为条件查询
                if(conditions==null){
                    //非排序非联表非条件查询----------------------------------------------------------------------------

                    for(int i=2;i<elementList.size();i++){
                        for(String s : select){
                            s=s.trim();
                            result=result+String.format(format,elementList.get(i).attributeValue(s));
                        }
                        result=result+"\n";
                    }
                    //非排序非联表非条件查询--------------------------------------------------------------------------------

                }else{
                //非排序非联表条件查询--------------------------------------------------------------------------------------
                    //判断是否包含and或者or
                    if(conditions.toLowerCase().contains("and")){

                        //and条件
                        String[] condition=conditions.split("and");
                        int k=0;
                        for(int i=2;i<elementList.size();i++){
                            int j=0;
                            for(String con:condition){
                                con=con.trim();
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

                            if(j==condition.length){
                                k++;
                                for(String s:select){
                                    s=s.trim();
                                    result=result+String.format(format,elementList.get(j).attributeValue(s));
                                }
                                result=result+"\n";
                            }
                        }
                        if(k==0){
                            return "修改的条件查找不到相应的列,无法修改";
                        }

                    }else if(conditions.toLowerCase().contains("or")){
                        //or条件
                        String[] condition=conditions.split("or");

                        for(String con:condition){
                            con=con.trim();
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
                                    for(String s:select){
                                        s=s.trim();
                                        result=result+String.format(format,elementList.get(i).attributeValue(s));
                                    }
                                    result=result+"\n";
                                }
                            }
                        }
                    }else{  //单条件
                        conditions=conditions.trim();
                        Matcher matcher1=pattern1.matcher(conditions);
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
                                for(String s:select){
                                    s=s.trim();
                                    result=result+String.format(format,elementList.get(i).attributeValue(s));
                                }
                                result=result+"\n";
                            }
                        }
                    }

                }
                //非排序非联表条件查询--------------------------------------------------------------------------------------


            }else{//是联表查询
            //联表条件查询------------------------------------------------------------------------------------------------
                String[] select = selects.split(",");
                for(String ss:select){
                    result=result+String.format(format,ss.trim());
                }
                result=result+"\n";



                //判断是否有表2的权限
                SAXReader reader_1 = new SAXReader();
                Document doc_1 = reader_1.read("src/main/resources/LoginList/userList.xml");
                List<Element> users_1 = doc.getRootElement().elements();
                for(Element user : users_1){
                    if(userName.equals(user.attributeValue("username"))){

                        String[] s=user.attributeValue("select").split(",");
                        boolean res = Arrays.asList(s).contains(tableName2);
                        if(!res){
                            return "用户不具有该表的select权限";
                        }
                    }
                }


                //判断表是否存在
                File file2=new File("src/main/resources/Database/"+tableName2.toLowerCase()+".xml");
                if(!file.exists()){
                    return "该表名不存在,无法查询";
                }

                SAXReader reader2 = new SAXReader();
                Document document2 = reader.read(file2);
                Element rootElement2 = document2.getRootElement();

                List<Element> elementList2 = rootElement2.elements();

                Matcher matcher2=pattern2.matcher(joinCondition);

                if(!matcher2.find()){
                    return "SQL语句语法不正确";
                }

                String attribute1=matcher2.group("attribute1");
                attribute1=attribute1.trim();
                String attribute2=matcher2.group("attribute2");
                attribute2=attribute2.trim();
                String tbName1=matcher2.group("tableName1");
                tbName1=tbName1.trim();
                String tbName2=matcher2.group("tableName2");
                tbName2=tbName2.trim();
                //测试return tbName1+"\n"+attribute1+"\n"+tbName2+"\n"+attribute2;

                if(!tbName1.equals(tableName1)){
                    String temp=attribute2;
                    attribute2=attribute1;
                    attribute1=temp;

                    temp=tbName2;
                    tbName2=tbName1;
                    tbName1=temp;
                }
                if(conditions==null){


                    //无条件连接查询
                    for(int i=2;i<elementList.size();i++){
                        for(int j=2;j<elementList2.size();j++){
                            if(elementList.get(i).attributeValue(attribute1).equals(elementList2.get(j).attributeValue(attribute2))){
                                for(String ss:select){
                                    ss=ss.trim();
                                    String[] s = ss.split("\\.");
                                    if(s[0].trim().equals(tableName1)){
                                        result=result+String.format(format,elementList.get(i).attributeValue(s[1].trim()));
                                    }else if(s[0].trim().equals(tableName2)){
                                        result=result+String.format(format,elementList2.get(j).attributeValue(s[1].trim()));
                                    }else{
                                        return "SQL语句出现错误";
                                    }
                                }
                                result=result+"\n";
                            }

                        }
                    }
                //联表条件查询
                } else{

                    //判断是否包含and或者or
                    if(conditions.toLowerCase().contains("and")){
                        //and条件
                        String[] condition=conditions.split("and");
                        int k=0;
                        for (int i = 2; i < elementList.size(); i++) {
                            for (int j = 2; j < elementList2.size(); j++) {
                                if (elementList.get(i).attributeValue(attribute1).equals(elementList2.get(j).attributeValue(attribute2))) {
                                    int num=0;
                                    for(String con:condition){
                                        con=con.trim();
                                        Matcher matcher3 = pattern3.matcher(con);
                                        if (!matcher3.find()) {
                                            return "SQL语句语法不正确";
                                        }
                                        String tbName = matcher3.group("tableName");
                                        tbName = tbName.trim();
                                        String attribute = matcher3.group("attribute");
                                        attribute = attribute.trim();
                                        String value = matcher3.group("value");
                                        value = value.trim();
                                        if(tbName.equals(tableName1)){
                                            if(elementList.get(i).attributeValue(attribute).equals(value)){
                                                num++;
                                            }else{
                                                break;
                                            }
                                        }else if(tbName.equals(tableName2)){
                                            if(elementList2.get(j).attributeValue(attribute).equals(value)){
                                                num++;
                                            }else{
                                                break;
                                            }
                                        }else{
                                            return "SQL语句出现错误";
                                        }
                                    }
                                    if(num==condition.length){
                                        k++;
                                        for (String ss : select) {
                                            ss = ss.trim();
                                            String[] s = ss.split("\\.");
                                            if (s[0].trim().equals(tableName1)) {
                                                result = result + String.format(format, elementList.get(i).attributeValue(s[1].trim()));
                                            } else if (s[0].trim().equals(tableName2)) {
                                                result = result + String.format(format, elementList2.get(j).attributeValue(s[1].trim()));
                                            } else {
                                                return "SQL语句出现错误";
                                            }
                                        }
                                        result = result + "\n";
                                    }


                                }
                            }
                        }
                        if(k==0){
                            return "修改的条件查找不到相应的列,无法修改";
                        }

                    }else if(conditions.toLowerCase().contains("or")){
                        //or条件
                        String[] condition=conditions.split("and");

                        for (int i = 2; i < elementList.size(); i++) {
                            for (int j = 2; j < elementList2.size(); j++) {
                                if(elementList.get(i).attributeValue(attribute1).equals(elementList2.get(j).attributeValue(attribute2))){
                                    for (String ss : select) {
                                        ss = ss.trim();
                                        String[] s = ss.split("\\.");
                                        if (s[0].trim().equals(tableName1)) {
                                            result = result + String.format(format, elementList.get(i).attributeValue(s[1].trim()));
                                        } else if (s[0].trim().equals(tableName2)) {
                                            result = result + String.format(format, elementList2.get(j).attributeValue(s[1].trim()));
                                        } else {
                                            return "SQL语句出现错误";
                                        }
                                    }
                                    result = result + "\n";
                                }
                            }
                        }

                    }else {  //单条件
                        conditions = conditions.trim();
                        Matcher matcher3 = pattern3.matcher(conditions);
                        if (!matcher3.find()) {
                            return "SQL语句语法不正确";
                        }
                        String tbName = matcher3.group("tableName");
                        tbName = tbName.trim();
                        String attribute = matcher3.group("attribute");
                        attribute = attribute.trim();
                        String value = matcher3.group("value");
                        value = value.trim();
                        //测试return tbName+"\n"+attribute+"\n"+value;

                        for (int i = 2; i < elementList.size(); i++) {
                            for (int j = 2; j < elementList2.size(); j++) {
                                if (elementList.get(i).attributeValue(attribute1).equals(elementList2.get(j).attributeValue(attribute2))) {
                                    if (tbName.equals(tableName1)) {
                                        if (elementList.get(i).attributeValue(attribute).equals(value)) {
                                            for (String ss : select) {
                                                ss = ss.trim();
                                                String[] s = ss.split("\\.");
                                                if (s[0].trim().equals(tableName1)) {
                                                    result = result + String.format(format, elementList.get(i).attributeValue(s[1].trim()));
                                                } else if (s[0].trim().equals(tableName2)) {
                                                    result = result + String.format(format, elementList2.get(j).attributeValue(s[1].trim()));
                                                } else {
                                                    return "SQL语句出现错误";
                                                }
                                            }
                                            result = result + "\n";
                                        }
                                        }else if (tbName.equals(tableName2)) {
                                            if (elementList2.get(j).attributeValue(attribute).equals(value)) {
                                                for (String ss : select) {
                                                    ss = ss.trim();
                                                    String[] s = ss.split("\\.");
                                                    if (s[0].trim().equals(tableName1)) {
                                                        result = result + String.format(format, elementList.get(i).attributeValue(s[1].trim()));
                                                    } else if (s[0].trim().equals(tableName2)) {
                                                        result = result + String.format(format, elementList2.get(j).attributeValue(s[1].trim()));
                                                    } else {
                                                        return "SQL语句出现错误";
                                                    }

                                                }
                                                result = result + "\n";
                                            }
                                        }
                                }
                            }
                        }
                    }
                }

            }
            //联表条件查询-----------------------------------------------------------------------------------------------

            //判断是否为排序查询
            if(orderAttribute!=null){
                orderAttribute=orderAttribute.trim();
                String[] attrs = orderAttribute.split("\\.");
                String[] rs = result.split("\n");
                String order_result=rs[0]+"\n";
                if(order.trim().equals("asc")){
                    //升序map
                    Map<Double,String> map=new TreeMap<Double,String>();
                    //非联表排序
                    int index=-1;//获取排序字段的下标值

                    String[] a = rs[0].split(" ");
                    for(int i=0;i<a.length;i++){
                        if(orderAttribute.equals(a[i].trim())){
                            index=i;
                        }
                    }
                    if(index==-1){
                        return "查询不到排序的字段";
                    }

                    for(int i=1;i<rs.length;i++){
                        String[] s1 = rs[i].trim().split(" ");
                        Double key=Double.parseDouble(s1[index].trim());
                        map.put(key,rs[i]+"\n");
                    }

                    //写入结果集
                    for (Map.Entry<Double, String> entry : map.entrySet()) {
                        //System.out.println(entry.getKey() + " " + entry.getValue());
                        order_result=order_result+entry.getValue();
                    }
                    return order_result;

                }else if(order.trim().equals("desc")){
                    //降序map
                    Map<Double,String> map = new TreeMap<Double,String>(Collections.reverseOrder());

                    //非联表排序
                    int index=-1;//获取排序字段的下标值

                    String[] a = rs[0].split(" ");
                    for(int i=0;i<a.length;i++){
                        if(orderAttribute.equals(a[i])){
                            index=i;
                        }
                    }
                    if(index==-1){
                        return "查询不到排序的字段";
                    }

                    for(int i=1;i<rs.length;i++){
                        String[] s1 = rs[i].trim().split(" ");
                        Double key=Double.parseDouble(s1[index].trim());
                        map.put(key,rs[i]+"\n");
                    }

                    //写入结果集
                    for (Map.Entry<Double, String> entry : map.entrySet()) {
                        //System.out.println(entry.getKey() + " " + entry.getValue());
                        order_result=order_result+entry.getValue();
                    }
                    return order_result;

                }

            }

            return result;



        }catch(Exception e){
            return "编译出现错误";
        }

    }

}

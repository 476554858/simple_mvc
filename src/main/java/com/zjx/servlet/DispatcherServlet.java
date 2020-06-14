package com.zjx.servlet;

import com.zjx.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/")
public class DispatcherServlet extends HttpServlet{

    List<String>  classNames = new ArrayList<String>();

    Map<String,Object> beans = new HashMap<String, Object>();

    Map<String,Method> handlerMap= new HashMap<String, Method>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //把所有的bean扫描----扫描所有的class文件
        scanPackage("com.zjx");

        //模拟bean的初始化
        doInstance();

        doIoc();
        //创建handlermapping
        buildUrlMapping();
    }

    private void scanPackage(String  basePackage){
        URL url = this.getClass().getClassLoader().getResource("/"+basePackage.replaceAll("\\.","/"));
        String fileStr = url.getFile();
        File file = new File(fileStr);
        String[] filesStr = file.list();

        for(String  path:filesStr){
            File filePath = new File(fileStr+path);
            if(filePath.isDirectory()){
                scanPackage(basePackage+"."+path);
            }else{
                classNames.add(basePackage+"."+filePath.getName());
            }
        }
    }

    private void doInstance(){
        if(classNames.size() == 0){
            System.out.println("包扫描失败===============");
            return;
        }

        //开始对各个注解执行各自的操作
        for(String className:classNames){
            String cn = className.replaceAll(".class","");

            try {
                Class<?> clazz = Class.forName(cn);
                if(clazz.isAnnotationPresent(MyController.class)){
                    Object instance = clazz.newInstance();

                    MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                    String rmvalue = requestMapping.value();
                    beans.put(rmvalue,instance);
                }else if(clazz.isAnnotationPresent(MyService.class)){
                    MyService myService = clazz.getAnnotation(MyService.class);
                    Object instance = clazz.newInstance();
                    beans.put(myService.value(),instance);
                }else{
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //做一些依赖的注入
    public void doIoc(){
        if(beans.size()==0){
            System.out.println("没有实例化类======");
            return;
        }

        for(Map.Entry<String,Object> entry:beans.entrySet()){
            Object instance = entry.getValue();
            Class<?> clazz  = instance.getClass();

            if(clazz.isAnnotationPresent(MyController.class)){
                Field[] fields = clazz.getDeclaredFields();
                for(Field  field:fields){
                    if(field.isAnnotationPresent(MyAutowired.class)){
                        MyAutowired auto = field.getAnnotation(MyAutowired.class);
                        String key = auto.value();
                        if("".equals(key)){
                            key = field.getName();
                        }
                        field.setAccessible(true);
                        try {
                            field.set(instance,beans.get(key));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void buildUrlMapping(){
        if(beans.size()==0){
            System.out.println("没有实例化类=====");
            return;
        }
        for(Map.Entry<String,Object> entry:beans.entrySet()){
            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();
            if(clazz.isAnnotationPresent(MyController.class)){
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                //拿到controller上requemapping中的值
                String classPath = requestMapping.value();
                Method[] methods = clazz.getMethods();
                for(Method method:methods){
                    if(method.isAnnotationPresent(MyRequestMapping.class)){
                        MyRequestMapping methodAnnotation = method.getAnnotation(MyRequestMapping.class);
                        String methodPath = methodAnnotation.value();
                        handlerMap.put(classPath+methodPath,method);
                    }
                }
            }
        }
    }

    private static Object[]  hand(HttpServletRequest req,HttpServletResponse resp,Method method){
        //拿到 当前方法有哪些参数
        Class<?>[] parameterTypes = method.getParameterTypes();
        //根据参数的个数,new 一个参数的数组,将方法里的所有参数赋值到args
        Object[] args = new Object[parameterTypes.length];

        int args_i= 0;
        int index= 0;
        for(Class<?> paramClazz:parameterTypes){
            if(ServletRequest.class.isAssignableFrom(paramClazz)){
                args[args_i++] = req;
            }
            if(ServletResponse.class.isAssignableFrom(paramClazz)){
                args[args_i++] = resp;
            }
            Annotation[] paramAns = method.getParameterAnnotations()[index];
            if(paramAns.length>0){
                for(Annotation paramAn:paramAns){
                    if(MyRequestParam.class.isAssignableFrom(paramAn.getClass())){
                        MyRequestParam rp = (MyRequestParam) paramAn;
                        args[args_i++] = req.getParameter(rp.value());
                    }
                }
            }
            index++;
        }
        return args;
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       //获取请求路径
        String uri= req.getRequestURI();
       String context= req.getContextPath();
       String path = uri.replaceAll(context,"");

        Method method = handlerMap.get(path);
        Object o = null;
        //拿到controller对象
        if(path.split("/").length>0){
            o = beans.get("/" + path.split("/")[1]);
        }
        if(o==null){
            return;
        }
        try {
            method.invoke(o,hand(req,resp,method));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}

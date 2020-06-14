package com.zjx.controller;

import com.zjx.annotation.MyAutowired;
import com.zjx.annotation.MyController;
import com.zjx.annotation.MyRequestMapping;
import com.zjx.annotation.MyRequestParam;
import com.zjx.service.ZjxService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@MyRequestMapping("/zjx")
@MyController
public class ZjxController {

    @MyAutowired
    ZjxService zjxService;

    @MyRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("name") String name
        , @MyRequestParam("age") String age){
        try {
            PrintWriter  pw = response.getWriter();
            String res = zjxService.query(name,age);
            pw.write(res);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.zjx.service.impl;

import com.zjx.annotation.MyRequestMapping;
import com.zjx.annotation.MyService;
import com.zjx.service.ZjxService;

@MyService("zjxService")
public class ZjxServiceImpl implements ZjxService {

    @Override
    public String query(String name, String age) {
        return "name="+name+"; age="+age;
    }
}

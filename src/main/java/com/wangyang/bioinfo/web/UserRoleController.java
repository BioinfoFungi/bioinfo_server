package com.wangyang.bioinfo.web;


import com.wangyang.bioinfo.pojo.authorize.RoleResource;
import com.wangyang.bioinfo.pojo.authorize.UserRole;
import com.wangyang.bioinfo.service.IRoleResourceService;
import com.wangyang.bioinfo.service.IUserRoleService;
import io.jsonwebtoken.lang.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/user_role")
public class UserRoleController {
    @Autowired
    IUserRoleService userRoleService;

    @PostMapping
    public UserRole save(@RequestBody UserRole userRole){
        return userRoleService.save(userRole);
    }


    @GetMapping("/del/{id}")
    public UserRole del(@PathVariable("id") Integer id){
        UserRole userRole = userRoleService.delBy(id);
        return userRole;
    }
}

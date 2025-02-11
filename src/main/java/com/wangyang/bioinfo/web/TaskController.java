package com.wangyang.bioinfo.web;

import com.alibaba.fastjson.JSONArray;
import com.wangyang.bioinfo.handle.CrudHandlers;
import com.wangyang.bioinfo.pojo.dto.TaskDto;
import com.wangyang.bioinfo.pojo.entity.Attachment;
import com.wangyang.bioinfo.pojo.entity.CancerStudy;
import com.wangyang.bioinfo.pojo.entity.Task;
import com.wangyang.bioinfo.pojo.authorize.User;
import com.wangyang.bioinfo.pojo.enums.CrudType;
import com.wangyang.bioinfo.pojo.param.AttachmentParam;
import com.wangyang.bioinfo.pojo.param.TaskParam;
import com.wangyang.bioinfo.pojo.param.TaskQuery;
import com.wangyang.bioinfo.service.task.ITaskService;
import com.wangyang.bioinfo.util.BaseResponse;
import joinery.DataFrame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * @author wangyang
 * @date 2021/7/22
 */
@RestController
@RequestMapping("/api/task")
public class TaskController {
    //http://localhost:8080/api/task?codeId=1&cancerStudyId=66&authorize=
    //ws://localhost:8080/websocket/socketServer.do
    //http://coolaf.com/tool/chattest
    @Autowired
    private ITaskService taskService;
    @Autowired
    private CrudHandlers crudHandlers;
    @PostMapping("/addTask/{crudEnum}")
    public Task addTask(@PathVariable(value = "crudEnum") CrudType crudEnum,
                        @RequestParam Integer id,
                        @RequestParam Integer codeId,
                        @RequestParam Integer taskId,
                        @RequestBody TaskDto taskDto,
                        HttpServletRequest request){
        User user = (User) request.getAttribute("user");
        return crudHandlers.addTask(crudEnum,taskId,id,codeId,taskDto,user);
    }
//    @GetMapping("/add/{cancerStudyId}")
//    public Task addTask(@PathVariable("cancerStudyId") Integer cancerStudyId) {
//        Task task = taskService.addTaskByCancerStudyId(cancerStudyId);
//        return task;
//    }

//    @GetMapping("/run")
//    public Task addTask(TaskParam taskParam, HttpServletRequest request){
//        User user = (User) request.getAttribute("user");
//        Task task = taskService.addTask(taskParam,user);
//        return task;
//    }
    @GetMapping("/runByCodeId/{id}")
    public BaseResponse runByCodeId(@PathVariable("id") Integer id, HttpServletRequest request){
        User user = (User) request.getAttribute("user");
        List<CancerStudy> cancerStudies = taskService.runByCodeId(id, user);
        return BaseResponse.ok("success",+cancerStudies.size());
    }

//    @GetMapping("/run/{id}")
//    public Task addTask(@PathVariable("id") Integer id, HttpServletRequest request){
//        User user = (User) request.getAttribute("user");
//        Task task = taskService.runTask(id,user);
//        return task;
//    }
    @GetMapping("/shutdown/{taskId}")
    public Task shutdownProcess(@PathVariable("taskId") int taskId){
        Task task = taskService.shutdownProcess(taskId);
        return task;
    }
    @GetMapping
    public Page<Task> page(TaskQuery taskQuery, @PageableDefault(sort = {"id"},direction = DESC) Pageable pageable){
        return taskService.page(taskQuery,pageable);
    }
    @GetMapping("/listAll/{crudEnum}")
    public List<Task> listAll(@PathVariable(value = "crudEnum") CrudType crudEnum,
                              Integer id){
        return taskService.listAll(crudEnum,id);
    }
    @GetMapping("/removeALlTask")
    public BaseResponse removeALlTask(){
        taskService.truncateTable();
        return BaseResponse.ok("清除成功！");
    }

    @GetMapping("/log")
    public  String getLogFiles(@RequestParam Integer taskId, @RequestParam(defaultValue = "20") Integer lines) {
        String logFiles = taskService.getLogFiles(taskId, lines);
        return logFiles;
    }

//    @GetMapping("/getObjMap/{id}")
//    public Map<String, String> getObjMap(@PathVariable("id") Integer id){
//        return  taskService.getObjMap(TaskType.CANCER_STUDY,id);
//    }

    @GetMapping("/del/{id}")
    public Task del(@PathVariable("id") Integer id){
        return  taskService.delBy(id);
    }

    @GetMapping("/findById/{id}")
    public Task findById(@PathVariable("id") Integer id){
        return taskService.findById(id);
    }


    @PostMapping(value = "/upload/{taskId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Task upload(@RequestParam("file") MultipartFile file, @PathVariable("taskId") Integer taskId, TaskParam taskParam, HttpServletRequest request){
        User user = (User) request.getAttribute("user");

        return  taskService.upload(file,user,taskId,taskParam);
    }
}

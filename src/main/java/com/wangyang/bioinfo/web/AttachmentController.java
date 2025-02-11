package com.wangyang.bioinfo.web;

import com.wangyang.bioinfo.pojo.enums.FileLocation;
import com.wangyang.bioinfo.pojo.entity.Attachment;
import com.wangyang.bioinfo.pojo.authorize.User;
import com.wangyang.bioinfo.pojo.param.AttachmentParam;
import com.wangyang.bioinfo.service.IAttachmentService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * @author wangyang
 * @date 2021/6/13
 */
@RestController
@RequestMapping("/api/attachment")
public class AttachmentController {

    @Autowired
    IAttachmentService attachmentService;

    @PostMapping
    public Attachment add(@RequestBody AttachmentParam attachmentParam, HttpServletRequest request){
        User user = (User) request.getAttribute("user");
        return attachmentService.saveAttachment(attachmentParam);
    }
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Attachment upload(@RequestParam("file") MultipartFile file, AttachmentParam attachmentParam, @NotNull HttpServletRequest request){
        User user = (User) request.getAttribute("user");
        return  attachmentService.upload(file,attachmentParam);
    }


    @PostMapping(value = "/uploadFiles/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Attachment upload(@PathVariable("id") int id,@RequestParam("file") MultipartFile file){
//        for (MultipartFile file : files) { //因为有上传多个文件，所以用的是MultipartFile[]数组，所以要遍历数组保存里面的每一个文件
//            String filePath = "D:/files/"; // 暂时设置保存在D盘的files目录下
//            System.out.println(" 文件名称： " + file.getOriginalFilename());
//            System.out.println(" 文件大小： " + file.getSize() / 1024D + "Kb");
//            System.out.println(" 文件类型： " + file.getContentType());
//            System.out.println();
//            //在这里执行调用文件保存的方法....
//            filePath = filePath + file.getOriginalFilename();
////            file.transferTo(new File(filePath));
//            filePath = "";
//            //其他业务代码如插入数据库代码省略.........
//        }
        Attachment attachment = attachmentService.upload(id, file);
        return attachment;
    }

    @GetMapping
    public Page<Attachment> page(Attachment baseFileQuery,@PageableDefault(sort = {"id"},direction = DESC) Pageable pageable){
        Page<Attachment> attachments = attachmentService.pageBy(baseFileQuery,"",pageable);
        return attachments;
    }

    @GetMapping("/page/{projectId}")
    public Page<Attachment> pageByProjectId(@PathVariable("projectId") int projectId, @PageableDefault(sort = {"id"},direction = DESC) Pageable pageable){
        Page<Attachment> attachments = attachmentService.pageAttachmentByProjectId(projectId,pageable);
        return attachments;
    }
    @GetMapping("/list/{projectId}")
    public List<Attachment> listByProjectId(@PathVariable("projectId") int projectId){
        List<Attachment> attachments = attachmentService.listAttachmentByProjectId(projectId);
        return attachments;
    }
    @GetMapping("/del/{id}")
    public Attachment delAttachment(@PathVariable("id") int id, HttpServletRequest request){
        User user = (User) request.getAttribute("user");
        return attachmentService.delAttachment(id,user);
    }
    @GetMapping("/downloadById/{Id}")
    public Attachment downloadById(@PathVariable("Id") Integer id,
                                    @RequestParam(value = "location",defaultValue = "LOCAL")FileLocation fileLocation,
                                   HttpServletResponse response){
        Attachment attachment = attachmentService.download(id, fileLocation,response);
        return attachment;
    }
    @GetMapping("/download/{uuid}")
    public Attachment download(@PathVariable("uuid") String uuid,
                                @RequestParam(value = "location",defaultValue = "LOCAL")FileLocation fileLocation,
                                HttpServletResponse response){
        Attachment attachment = attachmentService.download(uuid,fileLocation, response);
        return attachment;
    }

    @GetMapping("/findOne/{uuid}")
    public Attachment findByUUID(@PathVariable("uuid") String uuid){
        return attachmentService.findByUUID(uuid);
    }

}

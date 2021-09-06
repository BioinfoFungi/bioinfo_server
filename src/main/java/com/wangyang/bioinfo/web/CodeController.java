package com.wangyang.bioinfo.web;

import com.wangyang.bioinfo.pojo.authorize.User;
import com.wangyang.bioinfo.pojo.file.Code;
import com.wangyang.bioinfo.pojo.file.OrganizeFile;
import com.wangyang.bioinfo.pojo.file.TermMapping;
import com.wangyang.bioinfo.pojo.param.CodeParam;
import com.wangyang.bioinfo.pojo.param.CodeQuery;
import com.wangyang.bioinfo.pojo.support.FileContent;
import com.wangyang.bioinfo.pojo.support.FileTree;
import com.wangyang.bioinfo.service.ICodeService;
import com.wangyang.bioinfo.service.IOrganizeFileService;
import com.wangyang.bioinfo.util.BaseResponse;
import com.wangyang.bioinfo.util.CacheStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/code")
public class CodeController {

    @Autowired
    ICodeService codeService;

    @Autowired
    IOrganizeFileService organizeFileService;

    @GetMapping
    public Page<? extends TermMapping> page(CodeQuery codeQuery,
                                            @PageableDefault(sort = {"id"},direction = DESC) Pageable pageable,
                                            @RequestParam(value = "more", defaultValue = "false") Boolean more){
        Page<Code> cancerStudies = codeService.pageBy(codeQuery,pageable);
        if(more){
            return codeService.convertVo(cancerStudies);
        }
        return cancerStudies;
    }

    @GetMapping("listAllAnnTask")
    public List<Code>  listAllAnnTask(){
        List<Code> codes = codeService.listAllAnnTask();
        return codes;
    }

    @PostMapping
    public Code add(@RequestBody  CodeParam codeParam, HttpServletRequest request){
        User user = (User) request.getAttribute("user");
        Code code = codeService.saveBy(codeParam,user);
        return code;
    }

    @PostMapping("/update/{id}")
    public Code update(@PathVariable("id")Integer id, @RequestBody @Valid CodeParam codeParam, HttpServletRequest request){
        User user = (User) request.getAttribute("user");
        Code Code = codeService.updateBy(id,codeParam,user);
        return Code;
    }
    @GetMapping("/del/{id}")
    public Code del(@PathVariable("id") Integer id){
        return  codeService.delBy(id);
    }

    @GetMapping("/findByCan/{id}")
    public List<Code> findByCan(@PathVariable("id") Integer cancerId){
        return codeService.findByCan(cancerId);
    }

    @GetMapping("/checkFile/{id}")
    public Code checkFileExist(@PathVariable("id") Integer id){
        Code code = codeService.checkFileExist(id);
        return code;
    }
    @GetMapping("/findById/{id}")
    public TermMapping findById(@PathVariable("id") Integer id,
                                @RequestParam(value = "more", defaultValue = "false") Boolean more){
        Code code = codeService.findById(id);
        if(more){
            return codeService.convertVo(code);
        }
        return code;
    }

    @GetMapping("/init/{name}")
    public BaseResponse initData(@PathVariable("name") String name){
        OrganizeFile organizeFile = organizeFileService.findByEnName(name);
        codeService.initData(organizeFile.getAbsolutePath());
        return BaseResponse.ok("CancerStudy初始化完成!");
    }

    @GetMapping("/init")
    public BaseResponse initDataBy(@RequestParam(value = "path", defaultValue = "") String path){
        if(path!=null && path.equals("")){
            path = CacheStore.getValue("workDir")+"/TCGADOWNLOAD/data/Code.tsv";
        }
        codeService.initData(path);
        return BaseResponse.ok("["+path+"]初始化完成!");
    }
    @GetMapping("/file")
    public List<FileTree> listFiles(@RequestParam(value = "path", defaultValue = "")  String path){
        if(path!=null && path.equals("")){
            path = CacheStore.getValue("workDir")+"/TCGADOWNLOAD/R";
        }
        return codeService.listFiles(path);
    }



    @PostMapping("/file/save")
    public BaseResponse saveFileContent(@RequestBody FileContent fileContent){
        codeService.saveContent(fileContent.getPath(),fileContent.getContent());
        return BaseResponse.ok("保存文件成功！");
    }
    @GetMapping("/file/content")
    public BaseResponse getFileContent(@RequestParam("path") String path){
        return BaseResponse.ok("加载文件成功！",codeService.getFileContent(path));
    }
    @PostMapping("/createTSVFile")
    public void createTSVFile(HttpServletResponse response){
        codeService.createTSVFile(response);
    }
}

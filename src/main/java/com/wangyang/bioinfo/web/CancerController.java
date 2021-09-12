package com.wangyang.bioinfo.web;

import com.wangyang.bioinfo.pojo.annotation.Anonymous;
import com.wangyang.bioinfo.pojo.entity.AnalysisSoftware;
import com.wangyang.bioinfo.pojo.entity.OrganizeFile;
import com.wangyang.bioinfo.pojo.entity.Cancer;
import com.wangyang.bioinfo.pojo.authorize.User;
import com.wangyang.bioinfo.pojo.param.BaseTermParam;
import com.wangyang.bioinfo.pojo.param.CancerParam;
import com.wangyang.bioinfo.service.ICancerService;
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

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * @author wangyang
 * @date 2021/6/26
 */
@RestController
@RequestMapping("/api/cancer")
public class CancerController {
    @Autowired
    ICancerService cancerService;
    @Autowired
    IOrganizeFileService organizeFileService;

    @GetMapping
    @Anonymous
    public Page<Cancer> page(BaseTermParam baseTermParam, @PageableDefault(sort = {"id"},direction = DESC) Pageable pageable) {
        Page<Cancer> cancers = cancerService.pageBy(baseTermParam,pageable);
        return cancers;
    }

    @PostMapping
    public Cancer add(@RequestBody  CancerParam cancerParam, HttpServletRequest request){
        User user = (User) request.getAttribute("user");
        return  cancerService.add(cancerParam,user);
    }
    @PostMapping("/update/{id}")
    public Cancer update(@PathVariable("id") Integer id,@RequestBody  CancerParam cancerParam, HttpServletRequest request){
        User user = (User) request.getAttribute("user");
        return  cancerService.update(id,cancerParam,user);
    }
    @GetMapping("/del/{id}")
    public Cancer delById(@PathVariable("id")Integer id){
        return cancerService.delBy(id);
    }


    @PostMapping("/createTSVFile")
    public void createTSVFile(HttpServletResponse response){
        cancerService.createTSVFile(response);
    }

    @GetMapping("/listAll")
    public List<Cancer> listAll(){
        return cancerService.listAll();
    }



    @GetMapping("/init/{name}")
    public BaseResponse initData(@PathVariable("name") String name){
        OrganizeFile organizeFile = organizeFileService.findByEnName(name);
        cancerService.initData(organizeFile.getAbsolutePath(),false);
        return BaseResponse.ok("初始化完成!");
    }



    @GetMapping("/init")
    public BaseResponse initDataBy(@RequestParam(value = "path",defaultValue = "") String path,
                                   @RequestParam(value = "isEmpty", defaultValue = "false") Boolean isEmpty){
        if(path!=null && path.equals("")){
            path = CacheStore.getValue("workDir")+"/TCGADOWNLOAD/data/Cancer.tsv";
        }
        List<Cancer> cancerStudyList = cancerService.initData(path, isEmpty);
        return BaseResponse.ok("导入["+cancerStudyList.size()+"]个对象！");
    }

}

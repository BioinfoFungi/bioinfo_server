package com.wangyang.bioinfo.service;

import com.wangyang.bioinfo.pojo.entity.OrganizeFile;
import com.wangyang.bioinfo.pojo.param.OrganizeFileParam;
import com.wangyang.bioinfo.pojo.param.OrganizeFileQuery;
import com.wangyang.bioinfo.service.base.IBaseFileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author wangyang
 * @date 2021/7/8
 */
public interface IOrganizeFileService extends IBaseFileService<OrganizeFile> {

    OrganizeFile findByEnName(String enName);

    OrganizeFile save(OrganizeFileParam organizeFileParam);

    OrganizeFile upload(MultipartFile file, OrganizeFileParam organizeFileParam);

    Page<OrganizeFile> pageBy(OrganizeFileQuery organizeFileQuery, Pageable pageable);

}

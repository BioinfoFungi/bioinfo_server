package com.wangyang.bioinfo.repository.base;

import com.wangyang.bioinfo.pojo.entity.base.BaseFile;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author wangyang
 * @date 2021/7/8
 */
@NoRepositoryBean
public interface BaseFileRepository<FILE extends BaseFile> extends BaseRepository<FILE,Integer> {
}

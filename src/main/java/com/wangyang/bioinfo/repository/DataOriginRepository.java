package com.wangyang.bioinfo.repository;

import com.wangyang.bioinfo.pojo.DataOrigin;
import com.wangyang.bioinfo.repository.base.BaseTermRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author wangyang
 * @date 2021/6/26
 */
public interface DataOriginRepository extends BaseTermRepository<DataOrigin> {
}

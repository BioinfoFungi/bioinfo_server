package com.wangyang.bioinfo.service.base;

import com.wangyang.bioinfo.pojo.entity.base.BaseEntity;
import com.wangyang.bioinfo.pojo.enums.CrudType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * @author wangyang
 * @date 2021/6/27
 */
public interface ICrudService<DOMAIN extends BaseEntity, ID> {
    List<DOMAIN> listAll();
    DOMAIN add(@NonNull DOMAIN domain);
    DOMAIN save(@NonNull DOMAIN domain);

    @Transactional
    void truncateTable();

    DOMAIN findById(@NonNull ID id);

    void deleteAll();


    void delete(DOMAIN t);

    Page<DOMAIN> pageBy(Pageable pageable);

    void deleteAll(Iterable<DOMAIN> domains);

    void createTSVFile(HttpServletResponse response);

    List<DOMAIN> saveAll(Iterable<DOMAIN> domain);

    File createTSVFile(List<DOMAIN> domains, String filePath, String[] heads);

    List<DOMAIN> tsvToBean(String filePath);
    DOMAIN delBy(ID id);
    List<DOMAIN> initData(String filePath,Boolean isEmpty);
    boolean supportType(@Nullable CrudType type);

    List<String> getFields();
}

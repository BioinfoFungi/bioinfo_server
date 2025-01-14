package com.wangyang.bioinfo.service.impl;

import com.wangyang.bioinfo.pojo.entity.DataOrigin;
import com.wangyang.bioinfo.pojo.authorize.User;
import com.wangyang.bioinfo.pojo.enums.CrudType;
import com.wangyang.bioinfo.pojo.param.DataOriginParam;
import com.wangyang.bioinfo.repository.DataOriginRepository;
import com.wangyang.bioinfo.service.IDataOriginService;
import com.wangyang.bioinfo.service.base.BaseTermServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.List;

/**
 * @author wangyang
 * @date 2021/6/26
 */
@Service
@Transactional
public class DataOriginServiceImpl extends BaseTermServiceImpl<DataOrigin> implements IDataOriginService {

    private  final DataOriginRepository dataOriginRepository;

    public DataOriginServiceImpl(DataOriginRepository dataOriginRepository) {
        super(dataOriginRepository);
        this.dataOriginRepository=dataOriginRepository;
    }

    @Override
    public DataOrigin add(DataOriginParam dataOriginParam, User user) {
        DataOrigin dataOrigin = new DataOrigin();
        BeanUtils.copyProperties(dataOriginParam,dataOrigin);
        dataOrigin.setUserId(user.getId());
        return dataOriginRepository.save(dataOrigin);
    }

    @Override
    public DataOrigin update(Integer id, DataOriginParam dataOriginParam, User user) {
        DataOrigin dataOrigin = findById(id);
        BeanUtils.copyProperties(dataOriginParam,dataOrigin);
        dataOrigin.setUserId(user.getId());
        return dataOriginRepository.save(dataOrigin);
    }


    @Override
    public DataOrigin findDataOriginById(int id) {
        return null;
    }

    @Override
    public DataOrigin findDataOriginByEnName(String name) {
        List<DataOrigin> dataOrigins = dataOriginRepository.findAll(new Specification<DataOrigin>() {
            @Override
            public Predicate toPredicate(Root<DataOrigin> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaQuery.where(criteriaBuilder.equal(root.get("enName"),name)).getRestriction();
            }
        });
        if(dataOrigins.size()==0){
            return null;
        }
        return dataOrigins.get(0);
    }




    @Override
    public List<DataOrigin> listAll() {
        return dataOriginRepository.findAll();
    }

    @Override
    public Page<DataOrigin> pageDataOrigin(Pageable pageable) {
        return dataOriginRepository.findAll(pageable);
    }

    @Override
    public boolean supportType(CrudType type) {
        return false;
    }
}

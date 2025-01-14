package com.wangyang.bioinfo.pojo.entity.base;

import com.univocity.parsers.annotations.Parsed;
import com.wangyang.bioinfo.pojo.annotation.QueryField;
import com.wangyang.bioinfo.pojo.entity.base.BaseFile;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author wangyang
 * @date 2021/7/25
 */

@Data
@MappedSuperclass
//@Entity(name = "t_data_category")
//@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.INTEGER, columnDefinition = "int default 0")
public class TermMapping extends BaseEntity {


    @Parsed(field = "cancer")
    private Integer cancerId;
    @Parsed(field = "study")
    private Integer studyId;
    @Parsed(field = "dataOrigin")
    private Integer dataOriginId;
    @Parsed(field = "analysisSoftware")
    private Integer analysisSoftwareId;
    @Parsed(field = "dataCategory")
    private Integer dataCategoryId;
}

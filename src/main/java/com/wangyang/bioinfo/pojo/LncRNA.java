package com.wangyang.bioinfo.pojo;

import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @author wangyang
 * @date 2021/6/27
 */
@Entity
@DiscriminatorValue(value = "2")
@Data
public class LncRNA extends BaseRNA {

}

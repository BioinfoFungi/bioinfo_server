package com.wangyang.bioinfo.pojo;

import lombok.Data;

import javax.persistence.*;

/**
 * @author wangyang
 * @date 2021/6/27
 */
@Entity(name = "t_RNA")
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.INTEGER, columnDefinition = "int default 0")
@Data
public class BaseRNA extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String description;
}

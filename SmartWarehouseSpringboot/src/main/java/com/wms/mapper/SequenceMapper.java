package com.wms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SequenceMapper {

    Integer getNextVal(@Param("seqName") String seqName);

    int updateVal(@Param("seqName") String seqName, @Param("val") Integer val);
}

package com.oscar.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DbpingMapper {

    @Select("SELECT 1")
    Integer ping();
}

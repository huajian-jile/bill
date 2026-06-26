package org.example.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.bill.domain.Person;

@Mapper
public interface PersonMapper extends BaseMapper<Person> {}

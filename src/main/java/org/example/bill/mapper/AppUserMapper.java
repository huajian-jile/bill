package org.example.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.bill.domain.AppUser;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {}

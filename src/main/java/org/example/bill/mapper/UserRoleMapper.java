package org.example.bill.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleMapper {

    @Insert("INSERT INTO user_roles(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM user_roles WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}

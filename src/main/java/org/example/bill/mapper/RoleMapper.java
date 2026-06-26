package org.example.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.bill.domain.Role;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select(
            """
            SELECT r.* FROM roles r
            INNER JOIN user_roles ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            """)
    List<Role> selectRolesByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM roles WHERE UPPER(code) = UPPER(#{code}) LIMIT 1")
    Role selectByCode(@Param("code") String code);
}

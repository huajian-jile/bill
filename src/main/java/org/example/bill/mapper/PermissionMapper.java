package org.example.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.bill.domain.Permission;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select(
            """
            SELECT p.* FROM permissions p
            INNER JOIN role_permissions rp ON p.id = rp.permission_id
            WHERE rp.role_id = #{roleId}
            """)
    List<Permission> selectPermissionsByRoleId(@Param("roleId") Long roleId);
}

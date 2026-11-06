package com.codeagent.mapper;

import com.codeagent.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SysUserMapper {

    @Select("SELECT id, username, password, nickname, avatar, role, status, create_time, update_time, deleted " +
            "FROM sys_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    SysUser selectByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username} AND deleted = 0")
    long countByUsername(@Param("username") String username);

    @Select("SELECT id, username, password, nickname, avatar, role, status, create_time, update_time, deleted " +
            "FROM sys_user WHERE id = #{id} AND deleted = 0 LIMIT 1")
    SysUser selectById(@Param("id") Long id);

    @Insert("INSERT INTO sys_user (username, password, nickname, avatar, role, status, deleted, create_time, update_time) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{avatar}, #{role}, #{status}, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Update("UPDATE sys_user SET nickname = #{nickname}, avatar = #{avatar}, update_time = NOW() " +
            "WHERE id = #{id} AND deleted = 0")
    int updateProfile(@Param("id") Long id, @Param("nickname") String nickname, @Param("avatar") String avatar);
}

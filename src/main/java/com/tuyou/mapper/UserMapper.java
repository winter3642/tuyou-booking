package com.tuyou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuyou.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper，继承 BaseMapper 即获得单表 CRUD
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

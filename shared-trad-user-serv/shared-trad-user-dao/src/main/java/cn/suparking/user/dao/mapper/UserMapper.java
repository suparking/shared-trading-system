package cn.suparking.user.dao.mapper;

import cn.suparking.user.dao.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * User Mapper.
 */
@Mapper
public interface UserMapper {

    /**
     * select user by id.
     * @param id primary key
     * @return {@linkplain UserDO}
     */
    UserDO selectById(String id);

    /**
     * insert selective user.
     *
     * @param userDO {@linkplain UserDO}
     * @return rows
     */
    int insertSelective(UserDO userDO);

    /**
     * update selective user.
     *
     * @param userDO {@linkplain UserDO}
     * @return rows
     */
    int updateSelective(UserDO userDO);
}

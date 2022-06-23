package cn.suparking.customer.controller.cargroup.service;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.customer.api.beans.cargroup.CarGroupDTO;
import cn.suparking.customer.api.beans.cargroup.CarGroupQueryDTO;
import cn.suparking.customer.dao.entity.CarGroup;
import cn.suparking.customer.dao.vo.cargroup.CarGroupVO;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface CarGroupService {
    /**
     * 合约列表.
     *
     * @param carGroupQueryDTO {@link CarGroupQueryDTO}
     * @return {@link List}
     */
    PageInfo<CarGroupVO> list(CarGroupQueryDTO carGroupQueryDTO);

    /**
     * 合约添加.
     *
     * @param carGroupDTO {@link CarGroupDTO}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult insert(CarGroupDTO carGroupDTO);

    /**
<<<<<<< HEAD
     * 合约修改.
     *
     * @param carGroupDTO {@link CarGroupDTO}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult update(CarGroupDTO carGroupDTO);

    /**
=======
>>>>>>> 0e60ef51967f0d89fdcfe9d8918e5601cb43a29c
     * 合约删除.
     *
     * @param carGroupDTO {@link CarGroupDTO}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult remove(CarGroupDTO carGroupDTO);

    /**
     * 根据id查询合约信息.
     *
     * @param carGroupDTO {@link CarGroupDTO}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult findById(CarGroupDTO carGroupDTO);
}

package cn.suparking.order.dao.mapper;

import cn.suparking.order.api.beans.CarGroupRefundOrderQueryDTO;
import cn.suparking.order.dao.entity.CarGroupRefundOrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CarGroupRefundOrderMapper {

    /**
     * 合约退费订单列表.
     *
     * @param carGroupRefundOrderQueryDTO {@linkplain CarGroupRefundOrderQueryDTO}
     * @return {@linkplain CarGroupRefundOrderDO}
     */
    List<CarGroupRefundOrderDO> list(CarGroupRefundOrderQueryDTO carGroupRefundOrderQueryDTO);

    /**
     * 根据id查找合约退费订单.
     *
     * @param id primary id
     * @return {@linkplain CarGroupRefundOrderDO}
     */
    CarGroupRefundOrderDO selectById(String id);

    /**
     * 新增合约退费订单信息.
     *
     * @param carGroupRefundOrderDO {@linkplain CarGroupRefundOrderDO}
     * @return int
     */
    int insert(CarGroupRefundOrderDO carGroupRefundOrderDO);

    /**
     * 更新合约订单信息.
     *
     * @param carGroupRefundOrderDO {@linkplain CarGroupRefundOrderDO}
     * @return int
     */
    int update(CarGroupRefundOrderDO carGroupRefundOrderDO);
}

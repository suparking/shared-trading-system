package cn.suparking.order.dao.mapper;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.order.api.beans.ParkingRefundOrderQueryDTO;
import cn.suparking.order.dao.entity.ParkingRefundOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkingRefundOrderMapper {

    /**
     * 根据id查找临停退费订单.
     *
     * @param id primary id
     * @return {@linkplain ParkingRefundOrderDO}
     */
    ParkingRefundOrderDO selectById(String id);

    /**
     * 新增临停退费订单.
     *
     * @param parkingRefundOrderDO {@linkplain ParkingRefundOrderDO}
     * @return int
     */
    int insert(ParkingRefundOrderDO parkingRefundOrderDO);

    /**
     * 更新临停退费订单.
     *
     * @param parkingRefundOrderDO {@linkplain ParkingRefundOrderDO}
     * @return int
     */
    int update(ParkingRefundOrderDO parkingRefundOrderDO);

    /**
     * 根据原支付订单号获取数据.
     *
     * @param parkingRefundOrderQueryDTO {@link ParkingRefundOrderQueryDTO}
     * @return {@link SpkCommonResult}
     */
    ParkingRefundOrderDO getParkingRefundOrderByPayOrderNO(ParkingRefundOrderQueryDTO parkingRefundOrderQueryDTO);
}

package cn.suparking.order.service;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.order.api.beans.ParkingOrderDTO;
import cn.suparking.order.api.beans.ParkingQuery;
import cn.suparking.order.dao.entity.ParkingOrderDO;

import java.util.List;

public interface ParkingOrderService {

    /**
     * 根据查询临停订单信息.
     *
     * @param id 计费ID
     * @return ParkingOrderDO {@linkplain ParkingOrderDO}
     */
    ParkingOrderDO findById(String id);

    /**
     * 创建或修改临停订单.
     *
     * @param parkingOrderDTO 临停订单信息
     * @return Integer
     */
    Integer createOrUpdate(ParkingOrderDTO parkingOrderDTO);

    /**
     * 根据userId查询常去车场.
     *
     * @param userId 用户id
     * @param count 查询记录数
     * @return {@linkplain SpkCommonResult}
     */
    List<String> detailParkingOrder(Long userId, Integer count);

    /**
     *  findByUserIdsAndBeginTimeOrEndTimeRange.
     * @param parkingQuery {@link ParkingQuery}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult findByUserIdsAndBeginTimeOrEndTimeRange(ParkingQuery parkingQuery);

    /**
     * findByUserIdsAndEndTimeRange.
     * @param parkingQuery {@link ParkingQuery}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult findByUserIdsAndEndTimeRange(ParkingQuery parkingQuery);

    /**
     * findNextAggregateBeginTime.
     * @param parkingQuery {@link ParkingQuery}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult findNextAggregateBeginTime(ParkingQuery parkingQuery);
}

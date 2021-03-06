package cn.suparking.order.service.impl;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.order.api.beans.ParkingRefundOrderDTO;
import cn.suparking.order.api.beans.ParkingRefundOrderQueryDTO;
import cn.suparking.order.dao.entity.ParkingRefundOrderDO;
import cn.suparking.order.dao.mapper.ParkingRefundOrderMapper;
import cn.suparking.order.service.ParkingRefundOrderService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class ParkingRefundOrderServiceImpl implements ParkingRefundOrderService {

    private final ParkingRefundOrderMapper parkingRefundOrderMapper;

    public ParkingRefundOrderServiceImpl(final ParkingRefundOrderMapper parkingRefundOrderMapper) {
        this.parkingRefundOrderMapper = parkingRefundOrderMapper;
    }

    @Override
    public ParkingRefundOrderDO findById(final String id) {
        return parkingRefundOrderMapper.selectById(id);
    }

    @Override
    public Integer createOrUpdate(final ParkingRefundOrderDTO parkingRefundOrderDTO) {
        ParkingRefundOrderDO parkingRefundOrderDO = ParkingRefundOrderDO.buildParkingRefundOrderDO(parkingRefundOrderDTO);
        if (StringUtils.isEmpty(parkingRefundOrderDTO.getId())) {
            return parkingRefundOrderMapper.insert(parkingRefundOrderDO);
        }
        return parkingRefundOrderMapper.update(parkingRefundOrderDO);
    }

    /**
     * 根据原支付订单号获取数据.
     *
     * @param parkingRefundOrderQueryDTO {@link ParkingRefundOrderQueryDTO}
     * @return {@link SpkCommonResult}
     */
    @Override
    public SpkCommonResult getParkingRefundOrderByPayOrderNO(final ParkingRefundOrderQueryDTO parkingRefundOrderQueryDTO) {
        ParkingRefundOrderDO parkingRefundOrderDO = parkingRefundOrderMapper.getParkingRefundOrderByPayOrderNO(parkingRefundOrderQueryDTO);
        return SpkCommonResult.success(parkingRefundOrderDO);
    }
}

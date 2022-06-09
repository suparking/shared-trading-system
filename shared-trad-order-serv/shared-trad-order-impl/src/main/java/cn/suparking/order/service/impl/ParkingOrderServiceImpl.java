package cn.suparking.order.service.impl;

import cn.suparking.order.api.beans.ParkingOrderDTO;
import cn.suparking.order.service.ParkingOrderService;
import cn.suparking.order.dao.entity.ParkingOrderDO;
import cn.suparking.order.dao.mapper.ParkingOrderMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class ParkingOrderServiceImpl implements ParkingOrderService {

    private final ParkingOrderMapper parkingOrderMapper;

    public ParkingOrderServiceImpl(final ParkingOrderMapper parkingOrderMapper) {
        this.parkingOrderMapper = parkingOrderMapper;
    }

    @Override
    public ParkingOrderDO findById(final String id) {
        return parkingOrderMapper.selectById(id);
    }

    @Override
    public Integer createOrUpdate(final ParkingOrderDTO parkingOrderDTO) {
        ParkingOrderDO parkingOrderDO = ParkingOrderDO.buildParkingOrderDO(parkingOrderDTO);
        if (StringUtils.isEmpty(parkingOrderDTO.getId())) {
            return parkingOrderMapper.insert(parkingOrderDO);
        }
        return parkingOrderMapper.update(parkingOrderDO);
    }
}
package cn.suparking.data.service.impl;

import cn.suparking.data.api.beans.ParkingDTO;
import cn.suparking.data.dao.entity.ParkingDO;
import cn.suparking.data.dao.mapper.ParkingMapper;
import cn.suparking.data.service.ParkingService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class ParkingServiceImpl implements ParkingService {

    private final ParkingMapper parkingMapper;

    public ParkingServiceImpl(final ParkingMapper parkingMapper) {
        this.parkingMapper = parkingMapper;
    }

    @Override
    public ParkingDO findById(final String id) {
        return parkingMapper.selectById(id);
    }

    @Override
    public Integer createOrUpdate(final ParkingDTO parkingDTO) {
        ParkingDO parkingDO = ParkingDO.buildParkingDO(parkingDTO);
        if (StringUtils.isEmpty(parkingDTO.getId())) {
            return parkingMapper.insert(parkingDO);
        }
        return parkingMapper.update(parkingDO);
    }
}
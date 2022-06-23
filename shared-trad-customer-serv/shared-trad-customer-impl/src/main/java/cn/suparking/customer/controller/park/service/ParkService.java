package cn.suparking.customer.controller.park.service;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.customer.api.beans.ParkFeeQueryDTO;
import cn.suparking.customer.api.beans.ParkPayDTO;
import cn.suparking.customer.api.beans.ProjectQueryDTO;
import cn.suparking.customer.beans.park.LocationDTO;
import cn.suparking.customer.beans.park.RegularLocationDTO;
import cn.suparking.customer.vo.park.ParkInfoVO;

import java.util.List;

public interface ParkService {

    /**
     * 向B端 根据当前经纬度 获取最近的场库信息.
     * @param locationDTO {@link LocationDTO}
     * @return {@link List}
     */
    List<ParkInfoVO> nearByPark(LocationDTO locationDTO);

    /**
     * 向B端 查询所有场库列表.
     * @return {@link List}
     */
    List<ParkInfoVO> allLocation();

    /**
     * 查询设备计费信息.
     * @param sign sign
     * @param parkFeeQueryDTO {@link ParkFeeQueryDTO}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult scanCodeQueryFee(String sign, ParkFeeQueryDTO parkFeeQueryDTO);

    /**
     * 小程序下单接口.
     * @param sign C 端 下单签名
     * @param parkPayDTO {@link ParkPayDTO}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult miniToPay(String sign, ParkPayDTO parkPayDTO);

    /**
     * 根据用户ID获取常去的场库.
     * @param regularLocationDTO {@link RegularLocationDTO}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult regularByPark(RegularLocationDTO regularLocationDTO);

    /**
     * 根据设备编号查询项目信息.
     * @param sign C 端 使用 deviceNo 进行签名制作.
     * @param projectQueryDTO {@link ProjectQueryDTO}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult projectInfoByDeviceNo(String sign, ProjectQueryDTO projectQueryDTO);
}

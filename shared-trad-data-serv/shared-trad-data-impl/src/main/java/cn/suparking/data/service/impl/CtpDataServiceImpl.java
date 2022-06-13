package cn.suparking.data.service.impl;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.common.api.utils.HttpUtils;
import cn.suparking.data.api.beans.ParkStatusModel;
import cn.suparking.data.api.beans.ParkingLockModel;
import cn.suparking.data.api.beans.PublishData;
import cn.suparking.data.api.constant.DataConstant;
import cn.suparking.data.configuration.properties.SparkProperties;
import cn.suparking.data.mq.messagehandler.CTPMessageHandler;
import cn.suparking.data.service.CtpDataService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

@Slf4j
@Service
public class CtpDataServiceImpl implements CtpDataService {

    private final CTPMessageHandler ctpMessageHandler;

    @Resource
    private SparkProperties sparkProperties;

    public CtpDataServiceImpl(final CTPMessageHandler ctpMessageHandler) {
        this.ctpMessageHandler = ctpMessageHandler;
    }

    @Override
    public SpkCommonResult parkStatus(final JSONObject obj) {
        String body = obj.getString("message");
        if (StringUtils.isEmpty(body) || !obj.getString("type").equals("CTP")) {
            return SpkCommonResult.error("parkStatus Request Error");
        }
        PublishData publishData = JSON.parseObject(body, PublishData.class);
        String lockCode = "";
        ParkingLockModel parkingLockModel = null;
        ParkStatusModel parkStatusModel = JSON.parseObject(publishData.getData(), ParkStatusModel.class);
        lockCode = parkStatusModel.getLockCode();
        if (StringUtils.isNotBlank(lockCode)) {
            parkingLockModel = getParkInfoByDeviceNo(lockCode);
        }

        if (Objects.isNull(parkingLockModel)) {
            log.error("Shard Data ParkStatus parkingLockModel null");
            return SpkCommonResult.error("Shard Data ParkStatus parkingLockModel null");
        }
        return ctpMessageHandler.invoke(parkingLockModel, parkStatusModel);
    }


    /**
     * 根据设备编号获取车位信息.
     * @param deviceNo device no
     * @return {@link ParkingLockModel}
     */
    private ParkingLockModel getParkInfoByDeviceNo(final String deviceNo) {
        JSONObject request = new JSONObject();
        request.put("deviceNo", deviceNo);
        log.info("设备编号: " + deviceNo + ",请求车位信息发送报文: " + request.toJSONString());
        JSONObject result = HttpUtils.sendPost(sparkProperties.getUrl() + DataConstant.REQUEST_LOCK_DEVICE_RESOURCE, request.toJSONString());
        if (Objects.isNull(result) || !result.containsKey("code") || !result.getString("code").equals("00000")) {
            return null;
        }
        log.info("设备编号: " + deviceNo + ",接收车位信息报文: " + result.toJSONString());
        return JSON.parseObject(result.getString("data"), ParkingLockModel.class);
    }
}

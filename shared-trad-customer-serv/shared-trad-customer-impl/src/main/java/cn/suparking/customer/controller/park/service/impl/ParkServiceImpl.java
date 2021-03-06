package cn.suparking.customer.controller.park.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.common.api.configuration.SnowflakeConfig;
import cn.suparking.common.api.utils.DateUtils;
import cn.suparking.common.api.utils.HttpUtils;
import cn.suparking.common.api.utils.RandomCharUtils;
import cn.suparking.common.api.utils.SpkCommonResultMessage;
import cn.suparking.common.api.utils.Utils;
import cn.suparking.customer.api.beans.ParkFeeQueryDTO;
import cn.suparking.customer.api.beans.ParkPayDTO;
import cn.suparking.customer.api.beans.ProjectInfoQueryDTO;
import cn.suparking.customer.api.beans.ProjectQueryDTO;
import cn.suparking.customer.api.beans.discount.DiscountDTO;
import cn.suparking.customer.api.beans.discount.DiscountUsedDTO;
import cn.suparking.customer.api.beans.order.OrderDTO;
import cn.suparking.customer.api.beans.order.OrderQueryDTO;
import cn.suparking.customer.api.constant.ParkConstant;
import cn.suparking.customer.beans.park.LocationDTO;
import cn.suparking.customer.beans.park.RegularLocationDTO;
import cn.suparking.customer.configuration.properties.MiniProperties;
import cn.suparking.customer.configuration.properties.RabbitmqProperties;
import cn.suparking.customer.configuration.properties.SharedProperties;
import cn.suparking.customer.configuration.properties.SparkProperties;
import cn.suparking.customer.controller.park.service.OrderQueryService;
import cn.suparking.customer.controller.park.service.ParkService;
import cn.suparking.customer.dao.vo.user.ParkFeeQueryVO;
import cn.suparking.customer.feign.data.DataTemplateService;
import cn.suparking.customer.feign.user.UserTemplateService;
import cn.suparking.customer.spring.SharedTradCustomerInit;
import cn.suparking.customer.tools.OrderUtils;
import cn.suparking.customer.tools.ReactiveRedisUtils;
import cn.suparking.customer.vo.park.DeviceVO;
import cn.suparking.customer.vo.park.MiniPayVO;
import cn.suparking.customer.vo.park.ParkInfoVO;
import cn.suparking.customer.vo.park.ParkPayVO;
import cn.suparking.data.api.beans.ParkingLockModel;
import cn.suparking.data.api.beans.ProjectConfig;
import cn.suparking.data.api.parkfee.DiscountCustomer;
import cn.suparking.data.api.parkfee.DiscountInfo;
import cn.suparking.data.api.parkfee.ParkFeeRet;
import cn.suparking.data.api.parkfee.Parking;
import cn.suparking.data.api.parkfee.ParkingOrder;
import cn.suparking.data.api.query.ParkEventQuery;
import cn.suparking.data.api.query.ParkQuery;
import cn.suparking.data.dao.entity.DiscountInfoDO;
import cn.suparking.data.dao.entity.ParkingDO;
import cn.suparking.data.dao.entity.ParkingEventDO;
import cn.suparking.data.dao.entity.ParkingTriggerDO;
import cn.suparking.user.api.vo.UserVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.suparking.payutils.controller.ShuBoPaymentUtils;
import com.suparking.payutils.model.APICloseModel;
import com.suparking.payutils.model.APIOrderModel;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.suparking.customer.api.constant.ParkConstant.DISCOUNT_DELAY_TIME;
import static cn.suparking.customer.api.constant.ParkConstant.ORDER_TYPE;
import static cn.suparking.customer.api.constant.ParkConstant.PAYINFO_RESPONSE_QUEUE;
import static cn.suparking.customer.api.constant.ParkConstant.PAY_TERM_NO;
import static cn.suparking.customer.api.constant.ParkConstant.PAY_TYPE;
import static cn.suparking.customer.api.constant.ParkConstant.SUCCESS;
import static cn.suparking.customer.api.constant.ParkConstant.WETCHATMINI;

@Slf4j
@Service
public class ParkServiceImpl implements ParkService {

    @Resource
    private MiniProperties miniProperties;

    @Resource
    private SparkProperties sparkProperties;

    @Resource
    private RabbitmqProperties rabbitmqProperties;

    @Resource
    private SharedProperties sharedProperties;

    private final OrderServiceImpl orderService;

    private final RabbitTemplate rabbitTemplate;

    private final DataTemplateService dataTemplateService;

    private final UserTemplateService userTemplateService;

    public ParkServiceImpl(final DataTemplateService dataTemplateService, @Qualifier("MQCloudTemplate")final RabbitTemplate rabbitTemplate,
                           final UserTemplateService userTemplateService, final OrderServiceImpl orderService) {
        this.dataTemplateService = dataTemplateService;
        this.rabbitTemplate = rabbitTemplate;
        this.userTemplateService = userTemplateService;
        this.orderService = orderService;
    }

    @Override
    public List<ParkInfoVO> nearByPark(final LocationDTO locationDTO) {
        JSONObject requestParam = new JSONObject();
        requestParam.put("latitude", locationDTO.getLatitude());
        requestParam.put("longitude", locationDTO.getLongitude());
        requestParam.put("number", locationDTO.getNumber());
        requestParam.put("radius", locationDTO.getRadius());
        JSONObject result = HttpUtils.sendPost(sparkProperties.getUrl() + ParkConstant.INTERFACE_NEARBYPARK, requestParam.toJSONString());
        List<ParkInfoVO> parkInfoVOList = new LinkedList<>();
        return getParkInfoVOS(parkInfoVOList, result);
    }

    @Override
    public List<ParkInfoVO> allLocation() {
        JSONObject result = HttpUtils.sendGet(sparkProperties.getUrl() + ParkConstant.INTERFACE_ALLPARK, null);
        List<ParkInfoVO> parkInfoVOList = new LinkedList<>();
        return getParkInfoVOS(parkInfoVOList, result);
    }

    @Override
    public Map<String, ParkInfoVO> allLocationMap() {
        JSONObject result = HttpUtils.sendGet(sparkProperties.getUrl() + ParkConstant.INTERFACE_ALLPARK, null);
        Map<String, ParkInfoVO> parkInfoVOMap = new HashMap<>();
        return getParkInfoVOS(parkInfoVOMap, result);
    }

    @Override
    public SpkCommonResult scanCodeQueryFee(final String sign, final ParkFeeQueryDTO parkFeeQueryDTO) {
        // ?????? sign
        if (!invoke(sign, parkFeeQueryDTO.getDeviceNo())) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }
        // ??????????????????,?????????????????????
        if (StringUtils.isNotBlank(parkFeeQueryDTO.getDiscountNo())
                && StringUtils.isNotBlank(checkDiscountInfo(parkFeeQueryDTO.getDiscountNo()))) {
            return SpkCommonResult.error(SpkCommonResultMessage.DISCOUNT_ACTIVE);
        }

        // ????????????user id.
        UserVO userVO = userTemplateService.getUserByOpenId(parkFeeQueryDTO.getMiniOpenId());
        if (Objects.isNull(userVO)) {
            return SpkCommonResult.error(SpkCommonResultMessage.PARKING_DATE_USER_VALID);
        }
        parkFeeQueryDTO.setUserId(userVO.getId());

        // ?????? ??????
        ParkingLockModel parkingLockModel = dataTemplateService.findParkingLock(parkFeeQueryDTO.getDeviceNo());
        if (Objects.isNull(parkingLockModel)) {
            return SpkCommonResult.error(SpkCommonResultMessage.DEVICE_NOT_SETTING);
        }

        // ?????????????????????????????????.
        ParkQuery parkQuery = ParkQuery.builder()
                .projectId(Long.valueOf(parkingLockModel.getProjectId()))
                .projectNo(parkingLockModel.getProjectNo())
                .parkId(parkingLockModel.getId())
                .build();
        ParkingDO parkingDO = dataTemplateService.findParking(parkQuery);
        if (Objects.isNull(parkingDO)) {
            return SpkCommonResult.error(SpkCommonResultMessage.ENTER_PARKING_NOT_FOUND);
        }

        // ?????????????????????????????????.
        List<String> keys = getParkFeeRetKeys(parkingDO.getId() + "*");
        if (!keys.isEmpty()) {
            String key = keys.stream().filter(item -> item.contains(String.valueOf(parkingDO.getId()))).findFirst().get();
            String[] model = key.split("-");
            if (model.length != 2) {
                return SpkCommonResult.error(SpkCommonResultMessage.ORDER_VALID);
            }
            if (!model[1].equals(userVO.getId())) {
                return SpkCommonResult.error(SpkCommonResultMessage.ORDER_ACTIVE);
            }
        }

        ParkingTriggerDO parkingTriggerDO = null;
        if (Objects.nonNull(parkingDO.getEnter())) {
            parkingTriggerDO = dataTemplateService.findParkingTrigger(Long.valueOf(parkingLockModel.getProjectId()), parkingDO.getEnter());
            if (Objects.isNull(parkingTriggerDO)) {
                return SpkCommonResult.error(SpkCommonResultMessage.PARKING_DATA_TRIGGER_VALID);
            }
        }

        List<ParkingEventDO> parkingEvents = null;
        if (StringUtils.isNotBlank(parkingDO.getParkingEvents())) {
            String[] events = parkingDO.getParkingEvents().split(",");
            ParkEventQuery parkEventQuery = ParkEventQuery.builder()
                    .projectId(Long.valueOf(parkingLockModel.getProjectId()))
                    .ids(Stream.of(events).map(String::trim).filter(s -> !s.isEmpty())
                            .map(Long::valueOf).collect(Collectors.toList()))
                    .build();
            parkingEvents = dataTemplateService.findParkingEvents(parkEventQuery);
            if (Objects.isNull(parkingEvents) || parkingDO.getParkingEvents().split(",").length != parkingEvents.size()) {
                return SpkCommonResult.error(SpkCommonResultMessage.PARKING_DATA_EVENT_VALID);
            }
        }

        // ????????????????????? ProjectConfig
        ProjectConfig projectConfig = dataTemplateService.getProjectConfig(parkingLockModel.getProjectNo());
        if (Objects.isNull(projectConfig)) {
            return SpkCommonResult.error(SpkCommonResultMessage.PARKING_CONFIG_VALID);
        }

        JSONObject request = new JSONObject();

        // RPC ?????? ??????
        request.put("projectConfig", projectConfig);
        request.put("parking", parkingDO);
        // ????????????????????? ???????????????,?????????????????????, ????????????
        if (Objects.nonNull(parkFeeQueryDTO.getDiscountInfo())) {
            DiscountInfoDO discountInfoDO = DiscountInfoDO.builder()
                    .discountNo(parkFeeQueryDTO.getDiscountInfo().getDiscountNo())
                    .valueType(parkFeeQueryDTO.getDiscountInfo().getValueType())
                    .value(parkFeeQueryDTO.getDiscountInfo().getValue())
                    .quantity(parkFeeQueryDTO.getDiscountInfo().getQuantity())
                    .usedEndTime(parkFeeQueryDTO.getDiscountInfo().getUsedEndTime())
                    .usedEndTime(parkFeeQueryDTO.getDiscountInfo().getUsedEndTime())
                    .build();
            request.put("discountInfo", discountInfoDO);
        } else {
            if (StringUtils.isNotBlank(parkFeeQueryDTO.getDiscountNo())) {
                DiscountInfoDO discountInfoDO = getDiscountInfoByNo(parkFeeQueryDTO.getDiscountNo(), parkingDO.getProjectNo());
                request.put("discountInfo", Objects.nonNull(discountInfoDO)
                        ? discountInfoDO : null);
                // ?????????????????????????????????redis
                saveDiscountInfo(parkFeeQueryDTO.getDiscountNo(), parkFeeQueryDTO.getUserId());
            } else {
                request.put("discountInfo", null);
            }
        }

        request.put("userInfo", parkFeeQueryDTO);
        request.put("enter", parkingTriggerDO);
        request.put("parkingEvents", parkingEvents);
        //TODO ????????????.
        String retBody = sendRPCQueryFee(request);
        if (StringUtils.isBlank(retBody)) {
            return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_VALID);
        }

        // ????????????,???????????????
        ParkFeeRet parkFeeRet = JSON.parseObject(retBody, ParkFeeRet.class);
        if (Objects.isNull(parkFeeRet) || !parkFeeRet.getCode().equals("00000")) {
            return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_CHANGE_DATA_VALID);
        }
        // ?????????????????????
        String tmpOrderNo = String.valueOf(SnowflakeConfig.snowflakeId());
        log.info("??????: [" + parkFeeRet.getParking().getUserId() + " ], ????????????: [" + parkFeeRet.getParking().getDeviceNo() + " ],????????????????????????: [" + tmpOrderNo + " ]");
        parkFeeRet.setTmpOrderNo(tmpOrderNo);
        Parking parking = parkFeeRet.getParking();
        if (Objects.isNull(parking)) {
            return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_CHANGE_DATA_VALID);
        }

        ParkingOrder parkingOrder = parkFeeRet.getParkingOrder();
        if (Objects.isNull(parkingOrder)) {
            return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_CHANGE_DATA_VALID);
        }
        // ????????????????????????
        ParkFeeQueryVO parkFeeQueryVO = ParkFeeQueryVO.builder()
                .tmpOrderNo(tmpOrderNo)
                .parkingId(parking.getId())
                .projectNo(parking.getProjectNo())
                .userId(parking.getUserId().toString())
                .totalAmount(parkingOrder.getTotalAmount())
                .dueAmount(parkingOrder.getDueAmount())
                .discountedMinutes(parkingOrder.getDiscountedMinutes())
                .discountedAmount(parkingOrder.getDiscountedAmount())
                .carTypeClass(parkingOrder.getCarTypeClass())
                .carTypeName(parkingOrder.getCarTypeName())
                .beginTime(DateUtils.secondToDateTime(parkingOrder.getBeginTime()))
                .endTime(DateUtils.secondToDateTime(parkingOrder.getEndTime()))
                .parkingMinutes(DateUtils.formatSeconds(parkingOrder.getParkingMinutes() * 60))
                .paidAmount(parkingOrder.getPaidAmount())
                .parkNo(parking.getParkNo())
                .parkName(parking.getParkName())
                .parkId(parking.getParkId())
                .deviceNo(parking.getDeviceNo())
                .expireTime(DateUtils.secondToDateTime(parkingOrder.getExpireTime()))
                .bestBefore(DateUtils.secondToDateTime(parkingOrder.getBestBefore()))
                .expireTimeL(parkingOrder.getExpireTime())
                .build();

        if (Objects.nonNull(parkingOrder.getDiscountInfo())) {
            parkFeeQueryVO.setDiscountInfo(parkingOrder.getDiscountInfo());
        }

        // ??????UnionId ?????????????????????
        List<DiscountInfoDO> discountInfoDOList = getDiscountInfoListByUnionId(parking.getProjectNo(), parkFeeQueryDTO.getUnionId());
        if (Objects.nonNull(discountInfoDOList)) {
            parkFeeQueryVO.setDiscountInfoList(discountInfoDOList);
        }

        // ????????????????????? ???????????? ??????Redis
        opsValue(parkFeeRet, parking.getParkingConfig().getTxTTL() * 60);
        log.info("??????: [" + parking.getUserId() + " ]" + " ????????????????????????:??? [" + Utils.rmbFenToYuan(parkFeeQueryVO.getDueAmount()) + "] \n");
        log.info("??????: [" + parking.getUserId() + " ]" + " ??????????????????: [" + parkFeeQueryVO + " ] \n");
        return SpkCommonResult.success(parkFeeQueryVO);
    }

    @Override
    @GlobalTransactional(name = "shared-trad-customer-serv", rollbackFor = Exception.class)
    public SpkCommonResult miniToPay(final String sign, final ParkPayDTO parkPayDTO) {
        // ?????? sign
        if (!invoke(sign, parkPayDTO.getTmpOrderNo())) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }
        // ????????????????????????,??????????????????????????????????????????,??????????????????,????????????.
        ParkFeeRet parkFeeRet = getParkFeeRet(parkPayDTO.getParkingId() + "-" + parkPayDTO.getUserId());
        if (Objects.isNull(parkFeeRet)) {
            return SpkCommonResult.error(SpkCommonResultMessage.ORDER_EXPIRE);
        }
        Parking parking = parkFeeRet.getParking();
        ParkingOrder parkingOrder = parkFeeRet.getParkingOrder();
        if (Objects.isNull(parking) || Objects.isNull(parkingOrder)) {
            return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_CHANGE_DATA_VALID + "parking or parkingOrder is null");
        }
        // ????????????????????????????????????.
        if (StringUtils.isEmpty(parking.getId()) || Objects.isNull(parking.getUserId())) {
            return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_CHANGE_DATA_VALID + "parking.id or parking.userId is null");
        }
        // ????????????????????????.
        if (parkingOrder.getExpireTime() < DateUtil.currentSeconds()) {
            return SpkCommonResult.error(SpkCommonResultMessage.ORDER_EXPIRE);
        }
        MiniPayVO miniPayVO = MiniPayVO.builder().build();
        // ?????????????????????????????????.
        if (StringUtils.isNotEmpty(parkPayDTO.getDiscountNo())) {
            if ((StringUtils.isNotBlank(parkPayDTO.getDiscountNo()) && StringUtils.isNotBlank(parkPayDTO.getUserId())
                    && parkPayDTO.getUserId().equals(checkDiscountInfo(parkPayDTO.getDiscountNo()))
                    && saveDiscountInfo(parkPayDTO.getDiscountNo(), parkPayDTO.getUserId()))
                    || (StringUtils.isNotBlank(parkPayDTO.getDiscountNo()) && StringUtils.isEmpty(checkDiscountInfo(parkPayDTO.getDiscountNo()))
                    && StringUtils.isNotBlank(parkPayDTO.getUserId()) && saveDiscountInfo(parkPayDTO.getDiscountNo(), parkPayDTO.getUserId()))) {
                log.info("??????ID: " + parkPayDTO.getUserId() + " ?????????????????????: " + parkPayDTO.getDiscountNo() + " ??????????????????");
                miniPayVO.setDiscountDelayTime(String.valueOf(DISCOUNT_DELAY_TIME));
            } else {
                return SpkCommonResult.error(SpkCommonResultMessage.DISCOUNT_ACTIVE);
            }
        }

        // ?????????0,??????????????? -- ??????????????????.
        if (parkingOrder.getDueAmount() == 0) {
           // ???????????????.
            miniPayVO.setRetCode("0");
            miniPayVO.setNeedQuery(false);
            miniPayVO.setType(ORDER_TYPE);
            if (StringUtils.isNotBlank(parkPayDTO.getDiscountNo())) {
                if (deleteDiscountInfo(parkPayDTO.getDiscountNo())) {
                    log.info("?????????0,????????????,???????????? [" + parkPayDTO.getDiscountNo() + "] ????????????");
                }
            }

            String orderNo = OrderUtils.getOrderNo(parking.getProjectNo(), parkPayDTO.getTmpOrderNo());
            // ??????????????????????????????
            parkingOrder.setStatus("COMPLETE");
            // ????????????????????????
            if (orderService.saveOrder(parkingOrder, parking, orderNo, "CASH", PAY_TERM_NO, parkingOrder.getDueAmount(), "OFFLINE")) {
               // ??????????????????
                log.info("?????????: " + orderNo + "????????????, ??????????????????");
                if (orderService.openCtpDevice(parking.getDeviceNo())) {
                    log.info("??????ID: " + parking.getUserId() + "?????????: " + orderNo + " ????????????????????????");
                } else {
                    log.info("??????ID: " + parking.getUserId() + "?????????: " + orderNo + " ????????????????????????");
                }
                // ????????????????????????????????????????????????.
                DiscountInfo discountInfo = parkingOrder.getDiscountInfo();
                if (Objects.nonNull(discountInfo)) {
                    JSONObject discountInfoObj = new JSONObject();
                    discountInfoObj.put("discountNo", discountInfo.getDiscountNo());
                    discountInfoObj.put("valueType", discountInfo.getValueType());
                    discountInfoObj.put("value", discountInfo.getValue());
                    discountInfoObj.put("quantity", discountInfo.getQuantity());
                    discountInfoObj.put("usedStartTime", discountInfo.getUsedStartTime());
                    discountInfoObj.put("usedEndTime", discountInfo.getUsedEndTime());
                    DiscountUsedDTO discountUsedDTO = DiscountUsedDTO.builder()
                            .userId(parking.getUserId().toString())
                            .orderNo(orderNo)
                            .projectNo(parking.getProjectNo())
                            .discountInfo(discountInfoObj)
                            .build();
                    if (orderService.discountUsed(discountUsedDTO)) {
                        log.info("??????ID: " + parking.getUserId() + " ?????????: " + orderNo + " ?????????: " + discountInfo.getDiscountNo() + " ????????????");
                    } else {
                        log.info("??????ID: " + parking.getUserId() + " ?????????: " + orderNo + " ?????????: " + discountInfo.getDiscountNo() + " ????????????");
                    }
                }
            }
            return SpkCommonResult.success(miniPayVO);
        }
        // ??????????????????.
        String timeStart = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        // 2??????????????????
        Date expireDate = DateUtil.offsetMinute(new Date(), 2);
        // ?????????2?????????????????????
        String timeExpire = DateUtil.format(expireDate, "yyyyMMddHHmmss");

        APIOrderModel apiOrderModel = new APIOrderModel();
        apiOrderModel.setProjectNo(parking.getProjectNo());
        apiOrderModel.setTermInfo(PAY_TERM_NO);
        apiOrderModel.setTotalAmount(parkingOrder.getDueAmount());
        apiOrderModel.setTimeStart(timeStart);
        apiOrderModel.setTimeExpire(timeExpire);
        apiOrderModel.setProjectOrderNo(timeStart + ORDER_TYPE);
        apiOrderModel.setNotifyUrl("NO_NOTICE");
        apiOrderModel.setGoodsDesc(parking.getProjectNo() + "_" + parking.getDeviceNo() + "_" + parking.getParkName() + "????????????:"
                + NumberUtil.div(parkingOrder.getDueAmount().intValue(), 100, 2) + "???");
        apiOrderModel.setGoodsDetail(parking.getProjectNo() + "_" + parking.getDeviceNo() + "_" + parking.getParkName() + "????????????:"
                + NumberUtil.div(parkingOrder.getDueAmount().intValue(), 100, 2) + "???");
        apiOrderModel.setAttach(parking.getProjectNo() + "_" + parking.getDeviceNo() + "_" + parking.getParkName() + "_" + parking.getUserId());
        apiOrderModel.setSubject(parking.getProjectNo());
        apiOrderModel.setBusinessType("0".charAt(0));
        apiOrderModel.setAppid(miniProperties.getAppid());
        apiOrderModel.setSubopenid(parkPayDTO.getMiniOpenId());
        apiOrderModel.setTradetype(WETCHATMINI);
        log.info("?????????????????????????????? : " + JSON.toJSONString(apiOrderModel));
        String orderResultStr;
        JSONObject retJson;
        try {
            orderResultStr = ShuBoPaymentUtils.order(apiOrderModel);
            retJson = JSON.parseObject(orderResultStr);
        } catch (Exception e) {
            Arrays.stream(e.getStackTrace()).forEach(item -> log.error(item.toString()));
            return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_CHANGE_DATA_VALID + "????????????");
        }

        // ????????????,??????????????????.
        if (Objects.isNull(retJson)) {
            return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_CHANGE_DATA_VALID + "????????????,??????????????????");
        }
        String status = retJson.getString("status");
        if ("10008".equals(status)) {
            return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_CHANGE_DATA_VALID + "????????????,????????? 10008");
        }

        if ("512".equals(status)) {
            log.error("??????????????????????????????,????????? 512,?????????????????????");
            try {
                SharedTradCustomerInit.initPayTool(RandomCharUtils.getRandomChar(), log);
            } catch (Exception e) {
                Arrays.stream(e.getStackTrace()).forEach(item -> log.error(item.toString()));
            }
            return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_CHANGE_DATA_VALID + "????????????,????????? 512,?????????????????????");
        }

        if (!"200".equals(status)) {
            return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_CHANGE_DATA_VALID + "????????????,????????? " + status);
        }

        log.info("?????? " + parkPayDTO.getUserId() + " ?????????????????? : " + retJson);
        if (retJson.getString("status").equals("200")) {
            JSONObject result = retJson.getJSONObject("result");
            String resultCode = result.getString("result_code");
            if ("0".equals(resultCode)) {
                log.info("?????? " + parkPayDTO.getUserId() + " ????????????,????????? : " + result.getString("out_trade_no"));
                miniPayVO.setRetCode("0");
                miniPayVO.setNeedQuery(true);
                miniPayVO.setType(ORDER_TYPE);
                miniPayVO.setOutTradeNo(result.getString("out_trade_no"));
                miniPayVO.setPlatForm(result.getString("platform"));
                miniPayVO.setPayInfo(result.getString("payInfo"));
                miniPayVO.setDiscountDelayTime(String.valueOf(DISCOUNT_DELAY_TIME));

                if (OrderUtils.saveOrder(miniPayVO.getOutTradeNo())) {
                    parkingOrder.setStatus("RUNNING");
                    OrderQueryDTO orderQueryDTO = OrderQueryDTO.builder()
                            .orderNo(miniPayVO.getOutTradeNo())
                            .parking(parking)
                            .parkingOrder(parkingOrder)
                            .payType(PAY_TYPE)
                            .termNo(PAY_TERM_NO)
                            .amount(parkingOrder.getDueAmount())
                            .platForm(miniPayVO.getPlatForm())
                            .discountNo(parkPayDTO.getDiscountNo())
                            .build();
                    new OrderQueryService().queryOrder(orderQueryDTO);
                } else {
                    return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_CHANGE_DATA_VALID + "???????????????redis??????,????????????????????????.");
                }
            } else {
                return SpkCommonResult.error(SpkCommonResultMessage.CHARGE_CHANGE_DATA_VALID + "????????????,????????????: " + resultCode + "->" + result.getString("result_desc"));
            }
        }
        return SpkCommonResult.success(miniPayVO);
    }

    @Override
    public SpkCommonResult queryOrder(final String sign, final OrderDTO orderDTO) {
        // ?????? sign
        if (!invoke(sign, orderDTO.getOrderNo())) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }
        log.info("???????????????????????????????????? : " + JSON.toJSONString(orderDTO) + " ????????????: " + DateUtil.now());
        JSONObject resultObj = new JSONObject();
        String orderResultStr = OrderUtils.checkOrder(orderDTO.getOrderNo());
        if (StringUtils.isNotBlank(orderResultStr)) {
            if (orderResultStr.equals(orderDTO.getOrderNo())) {
                resultObj.put("query_code", "AA");
            } else {
                JSONObject retObj = JSONObject.parseObject(orderResultStr);
                String code = retObj.getString("code");
                if ("0".equals(code)) {
                    log.info("????????????????????????????????????,????????? : " + orderDTO.getOrderNo() + " ??????: " + DateUtil.now());
                    resultObj.put("query_code", "0");
                    if (StringUtils.isNotEmpty(orderDTO.getDiscountNo()) && deleteDiscountInfo(orderDTO.getDiscountNo())) {
                        log.info("???????????????????????????: " + orderDTO.getDiscountNo());
                    }
                    if (OrderUtils.deleteOrder(orderDTO.getOrderNo())) {
                        log.info("???????????????????????????,??????,????????? : " + orderDTO.getOrderNo());
                    }
                } else {
                    log.info("????????????????????????????????????,????????? : " + orderDTO.getOrderNo() + " ??????: " + DateUtil.now());
                    resultObj.put("query_code", code);
                    resultObj.put("query_msg", retObj.getString("msg"));
                }
            }
        } else {
            log.warn("Redis ??????????????? : " + orderDTO.getOrderNo() + " ???????????????");
            resultObj.put("query_code", "-1");
            resultObj.put("query_msg", "????????????");
        }
        return SpkCommonResult.success(resultObj);
    }

    @Override
    public SpkCommonResult closeOrder(final String sign, final OrderDTO orderDTO) {
        // ?????? sign
        if (!invoke(sign, orderDTO.getOrderNo())) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }
        log.info("???????????????????????????????????? : " + JSON.toJSONString(orderDTO) + " ????????????: " + DateUtil.now());
        APICloseModel closeModel = new APICloseModel();
        closeModel.setOrderNo(orderDTO.getOrderNo());
        closeModel.setProjectNo(orderDTO.getProjectNo());
        String resultStr = ShuBoPaymentUtils.close(closeModel);
        JSONObject resultObj = JSONObject.parseObject(resultStr);
        JSONObject retObj = new JSONObject();
        if (Objects.isNull(resultObj)) {
            log.info("?????????????????????,?????????null");
            retObj.put("code", "10002");
            retObj.put("msg", "????????????");
        }
        log.info("??????????????????????????? : " + resultStr);
        String status = resultObj.getString("status");
        if ("200".equals(status)) {
            JSONObject result = resultObj.getJSONObject("result");
            if ("0".equals(result.getString("result_code"))) {
                log.info("?????????????????????");
                retObj.put("code", "0");
            }
        } else if ("510".equals(status)) {
            log.info("status = 510, ???????????????????????????");
            retObj.put("code", "12022");
            retObj.put("msg", "???????????????");
        } else {
            log.info("?????????????????????");
            retObj.put("code", "10002");
            retObj.put("msg", "????????????");
        }
        OrderUtils.deleteOrder(orderDTO.getOrderNo());
        if (StringUtils.isNotBlank(orderDTO.getDiscountNo())) {
            deleteDiscountInfo(orderDTO.getDiscountNo());
        }
        return SpkCommonResult.success(retObj);
    }

    @Override
    public SpkCommonResult clearParkCache(final String sign, final ParkPayDTO parkPayDTO) {
        // ?????? sign
        if (!invoke(sign, parkPayDTO.getParkingId())) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }
        log.info("???????????????????????????????????? : " + JSON.toJSONString(parkPayDTO) + " ????????????: " + DateUtil.now());
        if (deleteParkFeeRet(parkPayDTO.getParkingId() + "-" + parkPayDTO.getUserId())) {
            log.info("???????????????????????????????????? : " + parkPayDTO.getParkingId() + " ??????ID : " + parkPayDTO.getUserId());
            return SpkCommonResult.success("??????????????????");
        } else {
            log.error("???????????????????????????????????? : " + parkPayDTO.getParkingId() + " ??????ID : " + parkPayDTO.getUserId());
            return SpkCommonResult.success("??????????????????");
        }
    }

    @Override
    public SpkCommonResult clearDiscountCache(final String sign, final DiscountDTO discountDTO) {
        // ?????? sign
        if (!invoke(sign, discountDTO.getDiscountNo())) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }
        log.info("????????????????????????????????????????????? : " + JSON.toJSONString(discountDTO) + " ????????????: " + DateUtil.now());
        if (deleteDiscountNo(discountDTO.getDiscountNo())) {
            log.info("???????????????????????????????????? : " + discountDTO.getDiscountNo());
            return SpkCommonResult.success("??????????????????");
        } else {
            log.info("???????????????????????????????????? : " + discountDTO.getDiscountNo());
            return SpkCommonResult.success("??????????????????");
        }
    }

    @Override
    public SpkCommonResult discountInfoByScanCode(final String sign, final DiscountDTO discountDTO) {
        // ?????? sign
        if (!invoke(sign, discountDTO.getDiscountNo())) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }
        log.info("????????????????????????????????????????????? : " + JSON.toJSONString(discountDTO) + " ????????????: " + DateUtil.now());
        if (StringUtils.isNotBlank(checkDiscountInfo(discountDTO.getDiscountNo()))) {
            return SpkCommonResult.error("????????????????????????");
        }
        DiscountInfoDO discountInfoDO = getDiscountInfoByNo(discountDTO.getDiscountNo(), discountDTO.getProjectNo());
        if (Objects.nonNull(discountInfoDO)) {
            log.info("?????????????????????????????????????????????,?????????redis : " + JSON.toJSONString(discountInfoDO));
            saveDiscountInfo(discountDTO.getDiscountNo(), JSON.toJSONString(discountInfoDO));
            return SpkCommonResult.success(discountInfoDO);
        } else {
            log.info("????????????????????????????????????????????? : " + JSON.toJSONString(discountInfoDO));
            return SpkCommonResult.error("??????????????????");
        }
    }

    @Override
    public SpkCommonResult regularByPark(final RegularLocationDTO regularLocationDTO) {
        JSONObject request = new JSONObject();
        request.put("userId", regularLocationDTO.getUserId());
        JSONObject result = HttpUtils.sendPost(sparkProperties.getUrl() + ParkConstant.INTERFACE_REGULARPARK, request.toJSONString());
        List<ParkInfoVO> parkInfoVOList = new LinkedList<>();
        return SpkCommonResult.success(getParkInfoVOS(parkInfoVOList, result));
    }

    @Override
    public SpkCommonResult projectInfoByDeviceNo(final String sign, final ProjectQueryDTO projectQueryDTO) {
        // ?????? sign
        if (!invoke(sign, projectQueryDTO.getDeviceNo())) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }
        JSONObject request = new JSONObject();
        request.put("deviceNo", projectQueryDTO.getDeviceNo());
        JSONObject result = HttpUtils.sendPost(sparkProperties.getUrl() + ParkConstant.INTERFACE_PARKBYDEVICE, request.toJSONString());
        return Optional.ofNullable(result).filter(res -> SUCCESS.equals(res.getString("code"))).map(item ->
                SpkCommonResult.success(JSON.parseObject(item.getString("data"), ParkPayVO.class))).orElseGet(() -> SpkCommonResult.error("????????????????????????"));
    }

    @Override
    public SpkCommonResult projectInfoByProjectNo(final String sign, final ProjectInfoQueryDTO projectInfoQueryDTO) {
        // ?????? sign
        if (!invoke(sign, projectInfoQueryDTO.getProjectNo())) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }
        JSONObject request = new JSONObject();
        request.put("projectNo", projectInfoQueryDTO.getProjectNo());
        JSONObject result = HttpUtils.sendPost(sparkProperties.getUrl() + ParkConstant.INTERFACE_PARKBYPROJECT, request.toJSONString());
        return Optional.ofNullable(result).filter(res -> SUCCESS.equals(res.getString("code"))).map(item ->
                SpkCommonResult.success(JSON.parseObject(item.getString("data"), ParkPayVO.class))).orElseGet(() -> SpkCommonResult.error("????????????????????????"));
    }

    @Override
    public SpkCommonResult getDeviceNo(final String sign, final ProjectInfoQueryDTO projectInfoQueryDTO) {
        // ?????? sign
        if (!invoke(sign, projectInfoQueryDTO.getParkNo())) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }
        JSONObject request = new JSONObject();
        request.put("projectNo", projectInfoQueryDTO.getProjectNo());
        request.put("parkNo", projectInfoQueryDTO.getParkNo());
        JSONObject result = HttpUtils.sendPost(sparkProperties.getUrl() + ParkConstant.INTERFACE_GETDEVICENO, request.toJSONString());
        return Optional.ofNullable(result).filter(res -> SUCCESS.equals(res.getString("code"))).map(item ->
                SpkCommonResult.success(JSON.parseObject(item.getString("data"), DeviceVO.class))).orElseGet(() -> SpkCommonResult.error("?????????????????????"));
    }

    @Override
    public SpkCommonResult getDiscountInfoCount(final String sign, final String unionId) {
        return SpkCommonResult.success(getDiscountInfoListByUnionId(null, unionId));
    }

    @Override
    public SpkCommonResult getDiscountInfo(final String sign, final String unionId) {
        return SpkCommonResult.success(getDiscountInfoListByUnionId(null, unionId));
    }

    private String sendRPCQueryFee(final JSONObject params) {
        log.info("Sender ??????RPC ??????,??????????????????: [" + params + "]");
        MessageProperties properties = new MessageProperties();
        properties.setHeader("method", "PARKING_ORDER_QUERY");
        properties.setHeader("timestamp", DateUtils.timestamp());
        properties.setReplyTo(PAYINFO_RESPONSE_QUEUE);
        Message message = new Message(params.toJSONString().getBytes(), properties);
        Object receiveMessage = rabbitTemplate.convertSendAndReceive(rabbitmqProperties.getExchange(), "*.shared.#", message);
        if (Objects.isNull(receiveMessage)) {
            log.error("sendRPCQueryFee Failed");
            return null;
        }
        JSONObject retJson = JSON.parseObject(new String((byte[]) receiveMessage));
        log.info("??????????????????????????????: [" + retJson.toJSONString() + "]");
        if (!retJson.containsKey("code") || !retJson.getString("code").equals("00000")) {
            return null;
        }
        return retJson.toJSONString();
    }

    /**
     * check sign.
     * @param sign sign.
     * @param deviceNo deviceNo
     * @return Boolean
     */
    private Boolean invoke(final String sign, final String deviceNo) {
        return md5(sharedProperties.getSecret() + deviceNo + DateUtils.currentDate() + sharedProperties.getSecret(), sign);
    }

    /**
     * MD5.
     * @param data the data
     * @param token the token
     * @return boolean
     */
    private boolean md5(final String data, final String token) {
        String keyStr = DigestUtils.md5Hex(data.toUpperCase()).toUpperCase();
        log.info("Mini MD5 Value: " + keyStr);
        if (keyStr.equals(token)) {
            return true;
        } else {
            log.warn("Mini Current MD5 :" + keyStr + ", Data Token : " + token);
        }
        return false;
    }

    /**
     * ????????????BS ???????????? C ?????????????????????.
     * @param parkInfoVOList {@link List}
     * @param result  {@link JSONObject}
     * @return {@link SpkCommonResult}
     */
    private List<ParkInfoVO> getParkInfoVOS(final List<ParkInfoVO> parkInfoVOList, final JSONObject result) {
        return Optional.ofNullable(result).filter(res -> SUCCESS.equals(res.getString("code"))).map(item -> {
            JSONArray jsonArray = item.getJSONArray("list");
            jsonArray.forEach(obj -> {
                try {
                    parkInfoVOList.add(JSON.parseObject(obj.toString(), ParkInfoVO.class));
                } catch (Exception e) {
                    Arrays.stream(e.getStackTrace()).forEach(err -> log.error(err.toString()));
                }
            });
            return parkInfoVOList;
        }).orElse(null);
    }

    /**
     * ????????????BS ???????????? C ?????????????????????.
     * @param parkInfoVOMap {@link List}
     * @param result  {@link JSONObject}
     * @return {@link SpkCommonResult}
     */
    private Map<String, ParkInfoVO> getParkInfoVOS(final Map<String, ParkInfoVO> parkInfoVOMap, final JSONObject result) {
        return Optional.ofNullable(result).filter(res -> SUCCESS.equals(res.getString("code"))).map(item -> {
            JSONArray jsonArray = item.getJSONArray("list");
            jsonArray.forEach(obj -> {
                try {
                    ParkInfoVO parkInfoVO = JSON.parseObject(obj.toString(), ParkInfoVO.class);
                    parkInfoVOMap.put(parkInfoVO.getProjectNo(), parkInfoVO);
                } catch (Exception e) {
                    Arrays.stream(e.getStackTrace()).forEach(err -> log.error(err.toString()));
                }
            });
            return parkInfoVOMap;
        }).orElse(null);
    }

    /**
     * save data.
     * @param parkFeeRet {@link ParkFeeRet}
     */
    private void opsValue(final ParkFeeRet parkFeeRet, final Integer expireTime) {
        ReactiveRedisUtils.putValue(parkFeeRet.getParking().getId() + "-" + parkFeeRet.getParking().getUserId(), JSON.toJSONString(parkFeeRet), expireTime).subscribe(
            flag -> {
                if (flag) {
                    log.info("?????????????????? Key= " + parkFeeRet.getParking().getId() + "-" + parkFeeRet.getParking().getUserId() + " save redis success! expireTime: " + expireTime);
                } else {
                    log.info("?????????????????? Key= " + parkFeeRet.getParking().getId() + "-" + parkFeeRet.getParking().getUserId() + " save redis failed!");
                }
            }
        );
    }





    /**
     * save discountInfo.
     * @param discountNo the discount
     * @param userId openId
     */
    private Boolean saveDiscountInfo(final String discountNo, final String userId) {
        return ReactiveRedisUtils.putValue(discountNo, userId, DISCOUNT_DELAY_TIME).block(Duration.ofMillis(3000));
    }

    /**
     * delete discountInfo.
     * @param discountNo the discount
     */
    private Boolean deleteDiscountInfo(final String discountNo) {
        return ReactiveRedisUtils.deleteValue(discountNo).block(Duration.ofMillis(3000));
    }

    /**
     * get discountNo openId.
     * @param discountNo the discountNo
     * @return userId
     */
    private String checkDiscountInfo(final String discountNo) {
        return (String) ReactiveRedisUtils.getData(discountNo).block();
    }

    /**
     * ????????????????????????,????????????,??????Redis ?????????.
     * @param parkingId {@link String} ????????????
     * @return {@link ParkFeeRet}
     */
    private ParkFeeRet getParkFeeRet(final String parkingId) {
        String parkFeeRetStr = (String) ReactiveRedisUtils.getData(parkingId).block(Duration.ofMillis(3000));
        if (StringUtils.isEmpty(parkFeeRetStr)) {
            log.info("???????????? Key= " + parkingId + " get redis failed!");
            return null;
        }
        return JSON.parseObject(parkFeeRetStr, ParkFeeRet.class);
    }

    /**
     * ??????redis???????????????????????????.
     * @param discountNo the discountNo
     * @return {@link Boolean}
     */
    private Boolean deleteDiscountNo(final String discountNo) {
        return ReactiveRedisUtils.deleteValue(discountNo).block(Duration.ofMillis(3000));
    }

    /**
     * ????????????????????????????????????.
     * @param parkingId String
     * @return Boolean
     */
    private Boolean deleteParkFeeRet(final String parkingId) {
        return ReactiveRedisUtils.deleteValue(parkingId).block(Duration.ofMillis(3000));
    }

    /**
     * ??????????????????Key.
     * @param keyPattern String
     * @return {@link List}
     */
    private List<String> getParkFeeRetKeys(final String keyPattern) {
        return ReactiveRedisUtils.getKeys(keyPattern).collectList().block(Duration.ofMillis(3000));
    }

    /**
     * ??????wx union id ???????????????.
     * @param unionId union id
     * @return {@link List}
     */
    private List<DiscountInfoDO> getDiscountInfoListByUnionId(final String projectNo, final String unionId) {
        Map<String, Object> params = new HashMap<>();
        String method = "discountUser/all";
        if (StringUtils.isNotBlank(projectNo)) {
            params.put("projectNo", projectNo);
            method = "discountUser/list";
        }
        params.put("appType", "wx");
        params.put("appUserId", unionId);
        params.put("listType", "notUse");
        log.info("??????????????????????????????: [ " + params + " ]");
        try {
            List<DiscountInfoDO> discountInfoDOList = new ArrayList<>();
            JSONObject result = HttpUtils.sendGet(sharedProperties.getDiscountUrl() + method, params);
            if (Objects.nonNull(result)) {
                List<JSONObject> discountObjList = JSONObject.parseArray(result.getString("notUse_list"), JSONObject.class);
                discountObjList.forEach(item -> {
                    JSONObject discount = item.getJSONObject("discount");
                    DiscountInfoDO discountInfoDO = DiscountInfoDO.builder()
                            .discountNo(discount.getString("discountNo"))
                            .value(discount.getInteger("value"))
                            .quantity(discount.getInteger("maxAvailableCount") - discount.getInteger("usedCount"))
                            .usedProjectNo(discount.getString("usedProjectNo"))
                            .expireDate(DateUtils.secondToDateTime(discount.getLong("expireDate")))
                            .valueType(discount.getString("valueType"))
                            .build();
                    JSONArray projectNames = item.getJSONArray("projectName");
                    if (Objects.nonNull(projectNames)) {
                        discountInfoDO.setProjectNos(projectNames.toString());
                    }
                    discountInfoDOList.add(discountInfoDO);
                });
            }
            if (discountInfoDOList.size() > 0) {
                return discountInfoDOList;
            }
        } catch (Exception ex) {
            log.error("?????????????????????????????????: " + ex);
        }
        return null;
    }

    /**
     * ??????????????????????????????????????????.
     *
     * @param discountNo discount no
     * @return String
     */
    private DiscountInfoDO getDiscountInfoByNo(final String discountNo, final String projectNo) {
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("discountNo", discountNo);
            log.info("?????????????????????????????????????????????????????? = [ " + param + " ]");
            JSONObject result = HttpUtils.sendGet(sharedProperties.getDiscountUrl() + "discount/getDiscountByNo", param);
            if (Objects.nonNull(result) && result.containsKey("code") && result.getString("code").equals("200")
                    && result.containsKey("data") && Objects.nonNull(result.getJSONObject("data"))) {
                log.info("???????????????????????? [ " + result.toJSONString() + " ]");
                DiscountCustomer discountCustomer = JSON.parseObject(result.getString("data"), DiscountCustomer.class);
                if (Objects.nonNull(discountCustomer) && discountCustomer.getEnabled()
                        && discountCustomer.getUsedCount() < discountCustomer.getMaxAvailableCount()
                        && discountCustomer.getProjectNo().equals(projectNo) && !judgeDiscountDate(discountCustomer.getExpireDate())) {
                    return DiscountInfoDO.builder()
                            .discountNo(discountCustomer.getDiscountNo())
                            .valueType(discountCustomer.getValueType())
                            .value(discountCustomer.getValue())
                            .quantity(1)
                            .build();

                } else {
                    log.error("?????????????????????: " + discountNo + " ???????????????????????????????????????: " + result.getString("data"));
                }
            }
        } catch (Exception e) {
            log.error("???????????????????????????????????????----------->msg = " + e);
        }
        return null;
    }

    /**
     * ???????????????????????????.
     * @param expireTime ????????????
     * @return {@link Boolean}
     */
    private Boolean judgeDiscountDate(final Long expireTime) {
        if (DateUtils.getCurrentSecond() > expireTime) {
            return true;
        }
        return false;
    }
}

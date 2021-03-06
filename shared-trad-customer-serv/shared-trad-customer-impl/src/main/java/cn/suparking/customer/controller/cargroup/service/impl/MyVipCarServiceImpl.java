package cn.suparking.customer.controller.cargroup.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.common.api.configuration.SnowflakeConfig;
import cn.suparking.common.api.exception.SpkCommonException;
import cn.suparking.common.api.utils.DateUtils;
import cn.suparking.common.api.utils.HttpRequestUtils;
import cn.suparking.common.api.utils.RandomCharUtils;
import cn.suparking.common.api.utils.SpkCommonResultMessage;
import cn.suparking.customer.api.beans.order.OrderDTO;
import cn.suparking.customer.api.beans.vip.VipOrderQueryDTO;
import cn.suparking.customer.api.beans.vip.VipPayDTO;
import cn.suparking.customer.api.constant.ParkConstant;
import cn.suparking.customer.configuration.properties.MiniProperties;
import cn.suparking.customer.configuration.properties.SharedProperties;
import cn.suparking.customer.configuration.properties.SparkProperties;
import cn.suparking.customer.controller.cargroup.service.MyVipCarService;
import cn.suparking.customer.controller.cargroup.service.VipOrderQueryService;
import cn.suparking.customer.dao.entity.CarGroup;
import cn.suparking.customer.dao.entity.CarGroupPeriod;
import cn.suparking.customer.dao.entity.CarGroupStockDO;
import cn.suparking.customer.dao.mapper.CarGroupMapper;
import cn.suparking.customer.dao.mapper.CarGroupPeriodMapper;
import cn.suparking.customer.dao.mapper.CarGroupStockMapper;
import cn.suparking.customer.dao.vo.cargroup.MyVipCarVo;
import cn.suparking.customer.dao.vo.cargroup.ProjectVipCarVo;
import cn.suparking.customer.dao.vo.cargroup.ProtocolVipCarVo;
import cn.suparking.customer.spring.SharedTradCustomerInit;
import cn.suparking.customer.tools.OrderUtils;
import cn.suparking.customer.tools.ReactiveRedisUtils;
import cn.suparking.customer.vo.park.MiniPayVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.suparking.payutils.controller.ShuBoPaymentUtils;
import com.suparking.payutils.model.APICloseModel;
import com.suparking.payutils.model.APIOrderModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static cn.suparking.customer.api.constant.ParkConstant.ORDER_TYPE;
import static cn.suparking.customer.api.constant.ParkConstant.PAY_TERM_NO;
import static cn.suparking.customer.api.constant.ParkConstant.PAY_TYPE;
import static cn.suparking.customer.api.constant.ParkConstant.WETCHATMINI;

@Slf4j
@Service
public class MyVipCarServiceImpl implements MyVipCarService {

    private final CarGroupMapper carGroupMapper;

    private final CarGroupPeriodMapper carGroupPeriodMapper;

    private final CarGroupStockMapper carGroupStockMapper;

    @Resource
    private SharedProperties sharedProperties;

    @Resource
    private SparkProperties sparkProperties;

    @Resource
    private MiniProperties miniProperties;

    public MyVipCarServiceImpl(final CarGroupMapper carGroupMapper, final CarGroupPeriodMapper carGroupPeriodMapper, final CarGroupStockMapper carGroupStockMapper) {
        this.carGroupMapper = carGroupMapper;
        this.carGroupPeriodMapper = carGroupPeriodMapper;
        this.carGroupStockMapper = carGroupStockMapper;
    }

    /**
     * ???????????????????????????????????????????????????.
     *
     * @param sign   ??????
     * @param userId ??????id
     * @return {@link SpkCommonResult}
     * @author ZDD
     * @date 2022/7/20 14:53:11
     */
    @Override
    public SpkCommonResult myVipCarList(final String sign, final String userId) {
        if (!invoke(sign, userId)) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }

        List<MyVipCarVo> list = new ArrayList<>();

        //????????????id??????????????????
        List<CarGroup> carGroupList = carGroupMapper.findByUserId(Long.valueOf(userId));

        //?????????????????????
        if (Objects.isNull(carGroupList)) {
            return SpkCommonResult.success(list);
        }

        for (CarGroup carGroup : carGroupList) {
            //??????????????????id??????????????????
            String protocolId = carGroup.getProtocolId();
            Map<String, Object> params = new HashMap<>();
            params.put("protocolId", protocolId);

            JSONObject result = HttpRequestUtils.sendGet(sparkProperties.getUrl() + ParkConstant.INTERFACE_MYVIPCARINFO, params);
            if (Objects.isNull(result) || !ParkConstant.SUCCESS.equals(result.getString("code"))) {
                log.warn("???????????????????????????????????? <====== ??????id [{}] ???????????????, ??????????????????", protocolId);
                continue;
            }
            JSONObject protocol = result.getJSONObject("protocol");

            //??????????????????????????????
            JSONArray userServicesArr = protocol.getJSONArray("userServices");
            List<String> userServices = JSONArray.parseArray(JSONObject.toJSONString(userServicesArr), String.class);
            //????????????????????????
            boolean canRenew = userServices.contains("RENEW");
            //???????????????????????? ?????????????????????null ???????????????
            List<CarGroupPeriod> carGroupPeriods = carGroupPeriodMapper.findByCarGroupId(carGroup.getId());
            CarGroupPeriod period = getEffectPeriod(carGroupPeriods);

            //????????????????????????
            List<CarGroupPeriod> futureList = getFutureList(carGroupPeriods);

            JSONObject project = result.getJSONObject("project");
            MyVipCarVo myVipCarVo = MyVipCarVo.builder().id(String.valueOf(carGroup.getId())).userId(userId)
                    .projectNo(carGroup.getProjectNo()).projectName(project.getString("projectName"))
                    .carTypeName(carGroup.getCarTypeName()).protocolId(protocolId)
                    .protocolName(carGroup.getProtocolName()).protocolDesc(protocol.getString("protocolDesc"))
                    .canRenew(canRenew).futureList(futureList).build();

            if (period != null) {
                myVipCarVo.setEndDate(period.getEndDate());
            }
            list.add(myVipCarVo);
        }
        return SpkCommonResult.success(list);
    }

    /**
     * ??????????????????????????????????????????.
     *
     * @return {@link SpkCommonResult}
     * @author ZDD
     * @date 2022/7/20 14:53:11
     */
    @Override
    public SpkCommonResult projectVipCarList(final String sign, final String projectNoParams) {
        if (!invoke(sign, projectNoParams)) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }
        List<ProjectVipCarVo> projectVipCarVoList = new ArrayList<>();

        //??????????????????????????????????????????
        JSONObject protocolListResult = HttpRequestUtils.sendGet(sparkProperties.getUrl() + ParkConstant.INTERFACE_NEWPROTOCOL, new HashMap<>());
        if (Objects.isNull(protocolListResult) || !ParkConstant.SUCCESS.equals(protocolListResult.getString("code"))) {
            log.warn("????????????????????????");
            throw new SpkCommonException("????????????????????????");
        }

        JSONArray protocolList = protocolListResult.getJSONArray("protocolList");
        if (Objects.isNull(protocolList)) {
            return SpkCommonResult.success(projectVipCarVoList);
        }

        //????????????????????????
        Set<String> projectNoList = new HashSet<>();
        for (int i = 0; i < protocolList.size(); i++) {
            JSONObject protocol = protocolList.getJSONObject(i);
            String projectNo = protocol.getString("projectNo");
            projectNoList.add(projectNo);
        }

        //??????????????????
        Map<String, Object> params = new HashMap<>();
        params.put("projectNoList", new ArrayList<>(projectNoList));
        JSONObject resultJSON = HttpRequestUtils.sendPost(sparkProperties.getUrl() + ParkConstant.INTERFACE_PROJECTLIST, params);
        if (Objects.isNull(resultJSON) || !ParkConstant.SUCCESS.equals(resultJSON.getString("code"))) {
            log.warn("????????????????????????");
            throw new SpkCommonException("????????????????????????");
        }

        JSONArray projectList = resultJSON.getJSONArray("projectList");
        if (Objects.isNull(projectList)) {
            return SpkCommonResult.success(projectVipCarVoList);
        }

        for (int i = 0; i < projectList.size(); i++) {
            JSONObject project = projectList.getJSONObject(i);
            ProjectVipCarVo projectVipCarVo = ProjectVipCarVo.builder().id(project.getString("id"))
                    .projectNo(project.getString("projectNo"))
                    .projectName(project.getString("projectName"))
                    .address(project.getString("addressSelect"))
                    .status("OPENING")
                    .build();

            JSONObject location = project.getJSONObject("location");
            if (Objects.nonNull(location)) {
                projectVipCarVo.setLongitude(location.getBigDecimal("longitude"));
                projectVipCarVo.setLatitude(location.getBigDecimal("latitude"));
            }
            if (Objects.nonNull(project.getJSONArray("openTime"))) {
                List<String> openTime = JSONArray.parseArray(JSONObject.toJSONString(project.getJSONArray("openTime")), String.class);
                projectVipCarVo.setOpenTime(openTime);
                projectVipCarVo.setStatus(openState(openTime) ? "OPENING" : "CLOSED");
            }
            projectVipCarVoList.add(projectVipCarVo);
        }
        return SpkCommonResult.success(projectVipCarVoList);
    }

    /**
     * ????????????????????????????????????.
     *
     * @param projectNo ????????????
     * @return {@link ProtocolVipCarVo}
     * @author ZDD
     * @date 2022/7/20 14:53:11
     */
    @Override
    public SpkCommonResult protocolVipCarList(final String sign, final String projectNo) {
        if (!invoke(sign, projectNo)) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }

        List<ProtocolVipCarVo> protocolVipCarVoList = new ArrayList<>();

        //????????????????????????????????????????????????
        Map<String, Object> params = new HashMap<>();
        params.put("projectNo", projectNo);
        JSONObject protocolListResult = HttpRequestUtils.sendGet(sparkProperties.getUrl() + ParkConstant.INTERFACE_NEWPROTOCOLBYPROJECTNO, params);
        if (Objects.isNull(protocolListResult) || !ParkConstant.SUCCESS.equals(protocolListResult.getString("code"))) {
            log.warn("????????????????????????");
            throw new SpkCommonException("????????????????????????");
        }

        JSONArray protocolList = protocolListResult.getJSONArray("protocolList");
        if (Objects.isNull(protocolList)) {
            return SpkCommonResult.success(protocolVipCarVoList);
        }

        for (int i = 0; i < protocolList.size(); i++) {
            JSONObject protocol = protocolList.getJSONObject(i);
            ProtocolVipCarVo protocolVipCarVo = ProtocolVipCarVo.builder().id(protocol.getString("id"))
                    .projectNo(protocol.getString("projectNo"))
                    .carTypeName(protocol.getString("carTypeName"))
                    .protocolId(protocol.getString("id"))
                    .protocolName(protocol.getString("protocolName"))
                    .protocolDesc(protocol.getString("protocolDesc"))
                    .price(protocol.getInteger("price"))
                    .stockQuantity(0)
                    .build();

            //??????????????????
            CarGroupStockDO carGroupStock = carGroupStockMapper.findByProtocolId(protocol.getString("id"));
            if (Objects.nonNull(carGroupStock)) {
                protocolVipCarVo.setStockId(carGroupStock.getId().toString());
                // ???????????? ?????? ?????????
                AtomicReference<Integer> tmpQuantity = new AtomicReference<>(0);
                List<String> keys = getStockGroupKeys(carGroupStock.getId() + "*");
                if (!keys.isEmpty()) {
                    keys.forEach(key -> {
                        tmpQuantity.updateAndGet(v -> v + getStockGroupQuantity(key));
                    });
                }
                protocolVipCarVo.setStockQuantity(carGroupStock.getStockQuantity() - tmpQuantity.get());
            }

            JSONObject duration = protocol.getJSONObject("duration");
            if (Objects.nonNull(duration)) {
                protocolVipCarVo.setDurationType(duration.getString("durationType"));
                protocolVipCarVo.setQuantity(duration.getInteger("quantity"));
            }

            protocolVipCarVoList.add(protocolVipCarVo);
        }
        return SpkCommonResult.success(protocolVipCarVoList);
    }

    @Override
    public SpkCommonResult carGroupToPay(final String sign, final VipPayDTO vipPayDTO) {
        // ?????? sign
        if (!invoke(sign, vipPayDTO.getStockId())) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }

        // TODO ???????????? ?????? ????????????????????????

        // ??????
        MiniPayVO miniPayVO = MiniPayVO.builder().build();
        // ?????????0,??????????????? -- ??????????????????.
        if (vipPayDTO.getDueAmount() == 0) {
            // ???????????????.
            miniPayVO.setRetCode("0");
            miniPayVO.setNeedQuery(false);
            miniPayVO.setType(ORDER_TYPE);
            String orderNo = OrderUtils.getOrderNo(vipPayDTO.getProjectNo(), String.valueOf(SnowflakeConfig.snowflakeId()));
            miniPayVO.setOutTradeNo(orderNo);

            // TODO ???????????????????????? ??? ?????? ????????????
            return SpkCommonResult.success(miniPayVO);
        }

        // ??????????????????.
        String timeStart = DateUtil.format(new Date(), "yyyyMMddHHmmss");

        // 2??????????????????
        Date expireDate = DateUtil.offsetMinute(new Date(), 2);

        // ?????????2?????????????????????
        String timeExpire = DateUtil.format(expireDate, "yyyyMMddHHmmss");

        APIOrderModel apiOrderModel = new APIOrderModel();
        apiOrderModel.setProjectNo(vipPayDTO.getProjectNo());
        apiOrderModel.setTermInfo(PAY_TERM_NO);
        apiOrderModel.setTotalAmount(vipPayDTO.getDueAmount());
        apiOrderModel.setTimeStart(timeStart);
        apiOrderModel.setTimeExpire(timeExpire);
        apiOrderModel.setProjectOrderNo(timeStart + ORDER_TYPE);
        apiOrderModel.setNotifyUrl("NO_NOTICE");
        apiOrderModel.setGoodsDesc("????????????_??????:" + vipPayDTO.getUserId() + ";??????:" + vipPayDTO.getProtocolName()
                + ";?????????:" + vipPayDTO.getBeginDate() + "~" + vipPayDTO.getEndDate());
        apiOrderModel.setGoodsDetail("????????????_??????:" + vipPayDTO.getUserId() + ";??????:" + vipPayDTO.getProtocolName()
                + ";?????????:" + vipPayDTO.getBeginDate() + "~" + vipPayDTO.getEndDate());
        apiOrderModel.setAttach(vipPayDTO.getProjectName());
        apiOrderModel.setSubject(vipPayDTO.getProjectNo());
        apiOrderModel.setBusinessType("1".charAt(0));
        apiOrderModel.setAppid(miniProperties.getAppid());
        apiOrderModel.setSubopenid(vipPayDTO.getMiniOpenId());
        apiOrderModel.setTradetype(WETCHATMINI);
        log.info("????????????????????????????????? : " + JSON.toJSONString(apiOrderModel));
        String orderResultStr;
        JSONObject retJson;
        try {
            orderResultStr = ShuBoPaymentUtils.order(apiOrderModel);
            retJson = JSON.parseObject(orderResultStr);
        } catch (Exception e) {
            Arrays.stream(e.getStackTrace()).forEach(item -> log.error(item.toString()));
            return SpkCommonResult.error(SpkCommonResultMessage.CAR_GROUP_DATA_VALID + "????????????");
        }

        // ????????????,??????????????????.
        if (Objects.isNull(retJson)) {
            return SpkCommonResult.error(SpkCommonResultMessage.CAR_GROUP_DATA_VALID + "????????????,??????????????????");
        }
        String status = retJson.getString("status");
        if ("10008".equals(status)) {
            return SpkCommonResult.error(SpkCommonResultMessage.CAR_GROUP_DATA_VALID + "????????????,????????? 10008");
        }

        if ("512".equals(status)) {
            log.error("??????????????????????????????,????????? 512,?????????????????????");
            try {
                SharedTradCustomerInit.initPayTool(RandomCharUtils.getRandomChar(), log);
            } catch (Exception e) {
                Arrays.stream(e.getStackTrace()).forEach(item -> log.error(item.toString()));
            }
            return SpkCommonResult.error(SpkCommonResultMessage.CAR_GROUP_DATA_VALID + "????????????,????????? 512,?????????????????????");
        }

        if (!"200".equals(status)) {
            return SpkCommonResult.error(SpkCommonResultMessage.CAR_GROUP_DATA_VALID + "????????????,????????? " + status);
        }

        log.info("?????? " + vipPayDTO.getUserId() + " ?????????????????? : " + retJson);
        if (retJson.getString("status").equals("200")) {
            JSONObject result = retJson.getJSONObject("result");
            String resultCode = result.getString("result_code");
            if ("0".equals(resultCode)) {
                log.info("?????? " + vipPayDTO.getUserId() + " ????????????,????????? : " + result.getString("out_trade_no"));
                miniPayVO.setRetCode("0");
                miniPayVO.setNeedQuery(true);
                miniPayVO.setType(ORDER_TYPE);
                miniPayVO.setOutTradeNo(result.getString("out_trade_no"));
                miniPayVO.setPlatForm(result.getString("platform"));
                miniPayVO.setPayInfo(result.getString("payInfo"));

                // TODO ??????????????????,????????????
                if (OrderUtils.saveOrder(miniPayVO.getOutTradeNo())) {
                    VipOrderQueryDTO vipOrderQueryDTO = VipOrderQueryDTO.builder()
                            .orderNo(miniPayVO.getOutTradeNo())
                            .payType(PAY_TYPE)
                            .termNo(PAY_TERM_NO)
                            .amount(vipPayDTO.getDueAmount())
                            .platForm(miniPayVO.getPlatForm())
                            .vipPayDTO(vipPayDTO)
                            .build();
                    new VipOrderQueryService().queryOrder(vipOrderQueryDTO);
                } else {
                    return SpkCommonResult.error(SpkCommonResultMessage.CAR_GROUP_DATA_VALID + "???????????????redis??????,????????????????????????.");
                }
            } else {
                return SpkCommonResult.error(SpkCommonResultMessage.CAR_GROUP_DATA_VALID + "????????????,????????????: " + resultCode + "->" + result.getString("result_desc"));
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
        log.info("????????????????????????????????? : " + JSON.toJSONString(orderDTO) + " ????????????: " + DateUtil.now());
        JSONObject resultObj = new JSONObject();
        String orderResultStr = OrderUtils.checkOrder(orderDTO.getOrderNo());
        if (StringUtils.isNotBlank(orderResultStr)) {
            if (orderResultStr.equals(orderDTO.getOrderNo())) {
                resultObj.put("query_code", "AA");
            } else {
                JSONObject retObj = JSONObject.parseObject(orderResultStr);
                String code = retObj.getString("code");
                if ("0".equals(code)) {
                    log.info("??????????????????????????????,????????? : " + orderDTO.getOrderNo() + " ??????: " + DateUtil.now());
                    resultObj.put("query_code", "0");
                    if (StringUtils.isNotEmpty(orderDTO.getStockKey()) && deleteStockInfo(orderDTO.getStockKey())) {
                        log.info("??????????????????????????????: " + orderDTO.getStockKey());
                    }
                    if (OrderUtils.deleteOrder(orderDTO.getOrderNo())) {
                        log.info("?????????????????????????????????,??????,????????? : " + orderDTO.getOrderNo());
                    }
                } else {
                    log.info("?????????????????????????????????,????????? : " + orderDTO.getOrderNo() + " ??????: " + DateUtil.now());
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
        log.info("????????????????????????????????? : " + JSON.toJSONString(orderDTO) + " ????????????: " + DateUtil.now());
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
        if (StringUtils.isNotBlank(orderDTO.getStockKey())) {
            deleteStockInfo(orderDTO.getStockKey());
        }
        return SpkCommonResult.success(retObj);
    }

    @Override
    public SpkCommonResult clearStockInfoCache(final String sign, final OrderDTO orderDTO) {
        // ?????? sign
        if (!invoke(sign, orderDTO.getStockKey())) {
            return SpkCommonResult.error(SpkCommonResultMessage.SIGN_NOT_VALID);
        }
        log.info("????????????????????????????????????????????? : " + JSON.toJSONString(orderDTO) + " ????????????: " + DateUtil.now());
        if (deleteStockInfo(orderDTO.getStockKey())) {
            log.info("??????????????????????????????????????? : " + orderDTO.getStockKey());
            return SpkCommonResult.success("??????????????????");
        } else {
            log.info("???????????????????????????????????? : " + orderDTO.getStockKey());
            return SpkCommonResult.success("??????????????????");
        }
    }

    /**
     * delete stockInfo.
     * @param stockKey the stock
     */
    private Boolean deleteStockInfo(final String stockKey) {
        return ReactiveRedisUtils.deleteValue(stockKey).block(Duration.ofMillis(3000));
    }
    /**
     * ??????????????????Key.
     * @param keyPattern String
     * @return {@link List}
     */
    private List<String> getStockGroupKeys(final String keyPattern) {
        return ReactiveRedisUtils.getKeys(keyPattern).collectList().block(Duration.ofMillis(3000));
    }

    /**
     * ??????Key ?????? ????????????.
     * @param key String
     * @return {@link Integer}
     */
    private Integer getStockGroupQuantity(final String key) {
        return (Integer) ReactiveRedisUtils.getData(key).block(Duration.ofMillis(3000));
    }

    /**
     * ?????????????????? ??????????????????????????????????????????.
     *
     * @param carGroupPeriodList {@linkplain CarGroupPeriod}
     * @return {@link CarGroupPeriod}
     * @author ZDD
     * @date 2022/7/20 16:58:25
     */
    private CarGroupPeriod getEffectPeriod(final List<CarGroupPeriod> carGroupPeriodList) {
        long nowTime = DateUtil.currentSeconds();
        //?????? ======> ????????????
        Collections.sort(carGroupPeriodList);
        for (CarGroupPeriod period : carGroupPeriodList) {
            Long beginDate = period.getBeginDate();
            Long endDate = period.getEndDate();
            //?????????
            if (nowTime >= beginDate && nowTime <= endDate) {
                //?????? ======> ?????????????????????
                return period;
            }
        }
        return null;
    }

    /**
     * ???????????? ======> ?????????????????? ?????????????????????????????????????????????.
     *
     * @param carGroupPeriodList {@linkplain CarGroupPeriod}
     * @return {@link CarGroupPeriod}
     * @author ZDD
     * @date 2022/7/20 16:58:25
     */
    private List<CarGroupPeriod> getFutureList(final List<CarGroupPeriod> carGroupPeriodList) {
        List<CarGroupPeriod> list = new ArrayList<>();
        Long nowTime = DateUtil.currentSeconds();
        //?????? ======> ????????????
        Collections.sort(carGroupPeriodList);
        for (CarGroupPeriod period : carGroupPeriodList) {
            Long beginDate = period.getBeginDate();
            if (beginDate > nowTime) {
                list.add(period);
            }
        }
        return list;
    }

    /**
     * ????????????.
     *
     * @param openTime ????????????
     * @return boolean
     * @author ZDD
     * @date 2022/7/20 20:38:07
     */
    private boolean openState(final List<String> openTime) {
        if (CollectionUtils.isEmpty(openTime) || openTime.size() < 2) {
            return false;
        }

        //???????????????????????????
        long currentMillis = 0L;
        long now = System.currentTimeMillis();
        SimpleDateFormat sdfOne = new SimpleDateFormat("yyyy-MM-dd");
        try {
            currentMillis = (now - (sdfOne.parse(sdfOne.format(now)).getTime())) / 1000;
        } catch (ParseException e) {
            log.warn("???????????????????????? [{}]", e.getMessage());
            e.printStackTrace();
        }

        String startTime = openTime.get(0);
        String endTime = openTime.get(1);

        //??????????????????????????????
        long startSecond = 0L;
        startSecond = startSecond + Integer.parseInt(startTime.split(":")[0]) * 60 * 60;
        startSecond = startSecond + Integer.parseInt(startTime.split(":")[1]) * 60;

        //??????????????????????????????
        long endSecond = 0L;
        endSecond = endSecond + Integer.parseInt(endTime.split(":")[0]) * 60 * 60;
        endSecond = endSecond + Integer.parseInt(endTime.split(":")[1]) * 60;

        //????????????
        if (startSecond > endSecond) {
            return currentMillis <= startSecond || currentMillis >= endSecond;
        } else {
            return currentMillis >= startSecond && currentMillis <= endSecond;
        }
    }

    /**
     * check sign.
     *
     * @param sign     sign.
     * @param deviceNo deviceNo
     * @return Boolean
     */
    private Boolean invoke(final String sign, final String deviceNo) {
        return md5(sharedProperties.getSecret() + deviceNo + DateUtils.currentDate() + sharedProperties.getSecret(), sign);
    }

    /**
     * MD5.
     *
     * @param data  the data
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
}

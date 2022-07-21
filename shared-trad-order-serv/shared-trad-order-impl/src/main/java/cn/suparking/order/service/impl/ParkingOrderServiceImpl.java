package cn.suparking.order.service.impl;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.order.api.beans.ParkingOrderDTO;
import cn.suparking.order.api.beans.ParkingOrderQueryDTO;
import cn.suparking.order.api.beans.ParkingQuery;
import cn.suparking.order.dao.entity.ChargeDetailDO;
import cn.suparking.order.dao.entity.ChargeInfoDO;
import cn.suparking.order.dao.entity.DiscountInfoDO;
import cn.suparking.order.dao.entity.ParkingOrderDO;
import cn.suparking.order.dao.mapper.ChargeDetailMapper;
import cn.suparking.order.dao.mapper.ChargeInfoMapper;
import cn.suparking.order.dao.mapper.DiscountInfoMapper;
import cn.suparking.order.dao.mapper.ParkingOrderMapper;
import cn.suparking.order.dao.vo.ChargeInfoVO;
import cn.suparking.order.dao.vo.ParkingOrderVO;
import cn.suparking.order.feign.order.UserTemplateService;
import cn.suparking.order.service.ParkingOrderService;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ParkingOrderServiceImpl implements ParkingOrderService {

    private final ChargeDetailMapper chargeDetailMapper;

    private final DiscountInfoMapper discountInfoMapper;

    private final ChargeInfoMapper chargeInfoMapper;

    private final ParkingOrderMapper parkingOrderMapper;

    private final UserTemplateService userTemplateService;

    public ParkingOrderServiceImpl(final ChargeDetailMapper chargeDetailMapper, final DiscountInfoMapper discountInfoMapper,
                                   final ChargeInfoMapper chargeInfoMapper, final UserTemplateService userTemplateService,
                                   final ParkingOrderMapper parkingOrderMapper) {
        this.chargeDetailMapper = chargeDetailMapper;
        this.discountInfoMapper = discountInfoMapper;
        this.chargeInfoMapper = chargeInfoMapper;
        this.userTemplateService = userTemplateService;
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

    /**
     * 根据userId查询常去车场.
     *
     * @param userId 用户id
     * @param count  查询记录数
     * @return {@linkplain SpkCommonResult}
     */
    @Override
    public List<String> detailParkingOrder(final Long userId, final Integer count) {
        PageHelper.startPage(1, count == null ? 5 : count);
        List<String> projectNoList = parkingOrderMapper.detailParkingOrder(userId);
        PageInfo<String> carGroupOrderDOPageInfo = new PageInfo<>(projectNoList);
        return carGroupOrderDOPageInfo.getList();
    }

    @Override
    public SpkCommonResult findByUserIdsAndBeginTimeOrEndTimeRange(final ParkingQuery parkingQuery) {
        Map<String, Object> params = new HashMap<>();
        params.put("projectNo", parkingQuery.getProjectNo());
        params.put("userIds", parkingQuery.getUserIds());
        params.put("begin", parkingQuery.getBegin());
        params.put("end", parkingQuery.getEnd());
        // 获取 ParkingOrderDO + ChargeInfo + DiscountInfo 信息
        List<ParkingOrderDO> parkingOrderDOList = parkingOrderMapper.findByUserIdsAndBeginTimeOrEndTimeRange(params);
        if (Objects.nonNull(parkingOrderDOList) && parkingOrderDOList.isEmpty()) {
            return SpkCommonResult.error("无订单信息");
        }
        List<ParkingOrderVO> parkingOrderVOList = new ArrayList<>(parkingOrderDOList.size());
        parkingOrderDOList.forEach(parkingOrderDO -> {
            ParkingOrderVO parkingOrderVO = new ParkingOrderVO();
            BeanUtils.copyProperties(parkingOrderDO, parkingOrderVO);
            DiscountInfoDO discountInfoDO = discountInfoMapper.findByParkingOrderId(parkingOrderDO.getId());
            parkingOrderVO.setDiscountInfoDO(discountInfoDO);
            List<ChargeInfoDO> chargeInfoDOList = chargeInfoMapper.findByParkingOrderId(parkingOrderDO.getId());
            LinkedList<ChargeInfoVO> chargeInfoVOList = new LinkedList<>();
            if (Objects.nonNull(chargeInfoDOList) && !chargeInfoDOList.isEmpty()) {
                chargeInfoDOList.forEach(chargeInfo -> {
                    ChargeInfoVO chargeInfoVO = new ChargeInfoVO();
                    BeanUtils.copyProperties(chargeInfo, chargeInfoVO);
                    LinkedList<ChargeDetailDO> chargeDetailDOList = chargeDetailMapper.findByChargeInfoId(chargeInfo.getId());
                    if (Objects.nonNull(chargeDetailDOList) && !chargeDetailDOList.isEmpty()) {
                        Collections.sort(chargeDetailDOList);
                        chargeInfoVO.setChargeDetailDOList(chargeDetailDOList);
                    }
                    chargeInfoVOList.add(chargeInfoVO);
                });
            }
            parkingOrderVO.setChargeInfos(chargeInfoVOList);
            parkingOrderVOList.add(parkingOrderVO);
        });

        return SpkCommonResult.success(parkingOrderVOList);
    }

    @Override
    public SpkCommonResult findByUserIdsAndEndTimeRange(final ParkingQuery parkingQuery) {
        Map<String, Object> params = new HashMap<>();
        params.put("projectNo", parkingQuery.getProjectNo());
        params.put("userIds", parkingQuery.getUserIds());
        params.put("begin", parkingQuery.getBegin());
        params.put("end", parkingQuery.getEnd());
        // 获取 ParkingOrderDO + ChargeInfo + DiscountInfo 信息
        List<ParkingOrderDO> parkingOrderDOList = parkingOrderMapper.findByUserIdsAndBeginTimeOrEndTimeRange(params);
        if (Objects.nonNull(parkingOrderDOList) && parkingOrderDOList.isEmpty()) {
            return SpkCommonResult.error("无订单信息");
        }
        List<ParkingOrderVO> parkingOrderVOList = new ArrayList<>(parkingOrderDOList.size());
        parkingOrderDOList.forEach(parkingOrderDO -> {
            ParkingOrderVO parkingOrderVO = new ParkingOrderVO();
            BeanUtils.copyProperties(parkingOrderDO, parkingOrderVO);
            DiscountInfoDO discountInfoDO = discountInfoMapper.findByParkingOrderId(parkingOrderDO.getId());
            parkingOrderVO.setDiscountInfoDO(discountInfoDO);
            List<ChargeInfoDO> chargeInfoDOList = chargeInfoMapper.findByParkingOrderId(parkingOrderDO.getId());
            LinkedList<ChargeInfoVO> chargeInfoVOList = new LinkedList<>();
            if (Objects.nonNull(chargeInfoDOList) && !chargeInfoDOList.isEmpty()) {
                chargeInfoDOList.forEach(chargeInfo -> {
                    ChargeInfoVO chargeInfoVO = new ChargeInfoVO();
                    BeanUtils.copyProperties(chargeInfo, chargeInfoVO);
                    LinkedList<ChargeDetailDO> chargeDetailDOList = chargeDetailMapper.findByChargeInfoId(chargeInfo.getId());
                    if (Objects.nonNull(chargeDetailDOList) && !chargeDetailDOList.isEmpty()) {
                        Collections.sort(chargeDetailDOList);
                        chargeInfoVO.setChargeDetailDOList(chargeDetailDOList);
                    }
                    chargeInfoVOList.add(chargeInfoVO);
                });
            }
            parkingOrderVO.setChargeInfos(chargeInfoVOList);
            parkingOrderVOList.add(parkingOrderVO);
        });

        return SpkCommonResult.success(parkingOrderVOList);
    }

    @Override
    public SpkCommonResult findNextAggregateBeginTime(final ParkingQuery parkingQuery) {
        Map<String, Object> params = new HashMap<>();
        params.put("projectNo", parkingQuery.getProjectNo());
        params.put("userIds", parkingQuery.getUserIds());

        ParkingOrderDO parkingOrderDO = parkingOrderMapper.findNextAggregateBeginTime(params);
        if (Objects.nonNull(parkingOrderDO)) {
            return SpkCommonResult.error("无订单信息");
        }
        List<ChargeInfoDO> chargeInfoDOList = chargeInfoMapper.findByParkingOrderId(parkingOrderDO.getId());
        LinkedList<ChargeInfoVO> chargeInfoVOList = new LinkedList<>();
        if (Objects.nonNull(chargeInfoDOList) && !chargeInfoDOList.isEmpty()) {
            chargeInfoDOList.forEach(chargeInfo -> {
                ChargeInfoVO chargeInfoVO = new ChargeInfoVO();
                BeanUtils.copyProperties(chargeInfo, chargeInfoVO);
                LinkedList<ChargeDetailDO> chargeDetailDOList = chargeDetailMapper.findByChargeInfoId(chargeInfo.getId());
                if (Objects.nonNull(chargeDetailDOList) && !chargeDetailDOList.isEmpty()) {
                    Collections.sort(chargeDetailDOList);
                    chargeInfoVO.setChargeDetailDOList(chargeDetailDOList);
                }
                chargeInfoVOList.add(chargeInfoVO);
            });
        }
        ParkingOrderVO parkingOrderVO = new ParkingOrderVO();
        BeanUtils.copyProperties(parkingOrderDO, parkingOrderVO);
        DiscountInfoDO discountInfoDO = discountInfoMapper.findByParkingOrderId(parkingOrderDO.getId());
        parkingOrderVO.setDiscountInfoDO(discountInfoDO);
        parkingOrderVO.setChargeInfos(chargeInfoVOList);
        return SpkCommonResult.success(parkingOrderVO);
    }

    /**
     * 根据条件查询订单.
     *
     * @param parkingOrderQueryDTO 订单详情信息
     * @return Integer
     */
    @Override
    public SpkCommonResult list(final ParkingOrderQueryDTO parkingOrderQueryDTO) {
        //如果手机号不为空，先根据手机号查询user表，获取userId
        String iphone = parkingOrderQueryDTO.getKeyword();
        if (!StringUtils.isBlank(iphone)) {
            JSONObject userByIphone = userTemplateService.getUserByIphone(iphone);
            if (userByIphone == null || userByIphone.getInteger("code") != 200 || ObjectUtils.isEmpty(userByIphone.getJSONObject("data"))) {
                return SpkCommonResult.success();
            }
            JSONObject data = userByIphone.getJSONObject("data");
            parkingOrderQueryDTO.setUserId(data.getLong("id"));
        }

        PageHelper.startPage(parkingOrderQueryDTO.getPage(), parkingOrderQueryDTO.getSize());
        List<ParkingOrderVO> parkingDOList = parkingOrderMapper.list(parkingOrderQueryDTO);
        parkingDOList.stream().forEach(item -> {
            //查询用户信息
            item.setPhone(getUserPhone(item.getUserId()));
            //查询优惠券信息
            item.setDiscountInfoDO(getDiscountInfo(item.getId()));
            //查询计费详情
            item.setChargeInfos(getChargeInfoVOList(item.getId()));
        });
        PageInfo<ParkingOrderVO> parkingOrderVOPageInfo = new PageInfo<>(parkingDOList);
        return SpkCommonResult.success(parkingOrderVOPageInfo);
    }

    /**
     * 根据订单id获取优惠券信息.
     *
     * @param userId 用户id
     * @return {@linkplain DiscountInfoDO}
     * @author ZDD
     * @date 2022/7/18 12:18:25
     */
    private String getUserPhone(final Long userId) {
        if (userId == null) {
            return "";
        }
        JSONObject userByIphone = userTemplateService.detailUser(userId);
        if (userByIphone == null || userByIphone.getInteger("code") != 200 || ObjectUtils.isEmpty(userByIphone.getJSONObject("data"))) {
            return "";
        }
        return userByIphone.getJSONObject("data").getString("iphone");
    }

    /**
     * 根据订单id获取优惠券信息.
     *
     * @param parkingOrderId 订单id
     * @return {@linkplain DiscountInfoDO}
     * @author ZDD
     * @date 2022/7/18 12:18:25
     */
    private DiscountInfoDO getDiscountInfo(final Long parkingOrderId) {
        return discountInfoMapper.findByParkingOrderId(parkingOrderId);
    }

    /**
     * 根据订单id获取优惠券信息.
     *
     * @param parkingOrderId 订单id
     * @return {@linkplain DiscountInfoDO}
     * @author ZDD
     * @date 2022/7/18 12:18:25
     */
    private LinkedList<ChargeInfoVO> getChargeInfoVOList(final Long parkingOrderId) {
        List<ChargeInfoDO> chargeInfoDOList = chargeInfoMapper.findByParkingOrderId(parkingOrderId);
        LinkedList<ChargeInfoVO> chargeInfoVOList = new LinkedList<>();
        if (Objects.nonNull(chargeInfoDOList) && !chargeInfoDOList.isEmpty()) {
            chargeInfoDOList.forEach(chargeInfo -> {
                ChargeInfoVO chargeInfoVO = new ChargeInfoVO();
                BeanUtils.copyProperties(chargeInfo, chargeInfoVO);
                LinkedList<ChargeDetailDO> chargeDetailDOList = chargeDetailMapper.findByChargeInfoId(chargeInfo.getId());
                if (Objects.nonNull(chargeDetailDOList) && !chargeDetailDOList.isEmpty()) {
                    Collections.sort(chargeDetailDOList);
                    chargeInfoVO.setChargeDetailDOList(chargeDetailDOList);
                }
                chargeInfoVOList.add(chargeInfoVO);
            });
        }
        return chargeInfoVOList;
    }
}

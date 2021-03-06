package cn.suparking.order.service;

import cn.suparking.order.api.beans.ChargeDetailDTO;
import cn.suparking.order.dao.entity.ChargeDetailDO;

public interface ChargeDetailService {

    /**
     * 根据查询计费详情.
     *
     * @param id 计费详情ID
     * @return ChargeDetailDO {@linkplain ChargeDetailDO}
     */
    ChargeDetailDO findById(String id);

    /**
     * 创建或修改计费详情信息.
     *
     * @param chargeDetailDTO 计费详情信息
     * @return Integer
     */
    Integer createOrUpdate(ChargeDetailDTO chargeDetailDTO);
}

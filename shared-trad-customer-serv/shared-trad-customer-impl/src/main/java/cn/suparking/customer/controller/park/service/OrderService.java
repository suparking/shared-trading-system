package cn.suparking.customer.controller.park.service;

import cn.suparking.data.api.parkfee.Parking;
import cn.suparking.data.api.parkfee.ParkingOrder;

public interface OrderService {

    /**
     * 存储订单数据.
     * @param parkingOrder {@link ParkingOrder}
     * @param parking {@link Parking}
     * @param orderNo {@link String} 订单号
     * @param payType {@link String} 支付类型
     * @param termNo {@link String} 终端号
     * @param amount  {@link Integer} 支付金额
     * @param plateForm {@link String} 支付平台
     * @return {@link Boolean}
     */
    Boolean saveOrder(ParkingOrder parkingOrder, Parking parking, String orderNo, String payType,
                      String termNo, Integer amount, String plateForm);

}

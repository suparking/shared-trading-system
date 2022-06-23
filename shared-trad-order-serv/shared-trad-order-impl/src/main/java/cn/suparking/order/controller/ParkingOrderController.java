package cn.suparking.order.controller;

import cn.suparking.order.api.beans.ParkingOrderDTO;
import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.common.api.utils.SpkCommonAssert;
import cn.suparking.common.api.utils.SpkCommonResultMessage;
import cn.suparking.order.service.ParkingOrderService;
import cn.suparking.order.dao.entity.ParkingOrderDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@Slf4j
@RefreshScope
@RestController
@RequestMapping("parking-order")
public class ParkingOrderController {
    private final ParkingOrderService parkingOrderService;

    public ParkingOrderController(final ParkingOrderService parkingOrderService) {
        this.parkingOrderService = parkingOrderService;
    }

    /**
     * 根据订单id查询订单信息.
     *
     * @param id 退费订单id
     * @return {@linkplain SpkCommonResult}
     */
    @GetMapping("/{id}")
    public SpkCommonResult detailParkingOrder(@PathVariable("id") final String id) {
        ParkingOrderDO parkingOrderDO = parkingOrderService.findById(id);
        return Optional.ofNullable(parkingOrderDO)
                .map(item -> SpkCommonResult.success(SpkCommonResultMessage.DETAIL_SUCCESS, item))
                .orElseGet(() -> SpkCommonResult.error("订单信息不存在"));
    }

    /**
     * 创建或修改订单信息.
     *
     * @param parkingOrderDTO 订单详情信息
     * @return Integer
     */
    @PostMapping("")
    public SpkCommonResult createParkingOrder(@Valid @RequestBody final ParkingOrderDTO parkingOrderDTO) {
        return Optional.ofNullable(parkingOrderDTO)
                .map(item -> {
                    SpkCommonAssert.notBlank(item.getOrderNo(), "订单号不能为空");
                    Integer count = parkingOrderService.createOrUpdate(item);
                    return SpkCommonResult.success(SpkCommonResultMessage.CREATE_SUCCESS, count);
                }).orElseGet(() -> SpkCommonResult.error("订单信息不存在"));
    }

    /**
     * 根据userId查询常去车场.
     *
     * @param userId 用户id
     * @param count 查询记录数
     * @return {@linkplain SpkCommonResult}
     */
    @GetMapping("/regularLocations")
    public SpkCommonResult detailParkingOrder(@RequestParam final Long userId,@RequestParam final Integer count) {
        List<String> projectNoList = parkingOrderService.detailParkingOrder(userId,count);
        return Optional.ofNullable(projectNoList)
                .map(item -> SpkCommonResult.success(SpkCommonResultMessage.DETAIL_SUCCESS, item))
                .orElseGet(() -> SpkCommonResult.error("订单信息不存在"));
    }
}

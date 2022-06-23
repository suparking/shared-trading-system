package cn.suparking.customer.feign.cargroupstocklog;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.customer.api.beans.cargroupstock.CarGroupStockOperateRecordDTO;
import cn.suparking.customer.api.beans.cargroupstock.CarGroupStockOperateRecordQueryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "shared-trad-data-serv", path = "/data-center/car-group-stock-log")
public interface CarGroupStockLogTemplateService {

    /**
     * 新增合约库存.
     *
     * @param carGroupStockOperateRecordQueryDTO {@link CarGroupStockOperateRecordQueryDTO}
     * @return {@linkplain SpkCommonResult}
     */
    @PostMapping("/list")
    SpkCommonResult list(@RequestBody CarGroupStockOperateRecordQueryDTO carGroupStockOperateRecordQueryDTO);

    /**
     * 新增合约库存.
     *
     * @param carGroupStockOperateRecordDTO {@link CarGroupStockOperateRecordDTO}
     * @return {@linkplain SpkCommonResult}
     */
    @PostMapping("/insert")
    SpkCommonResult insert(@RequestBody CarGroupStockOperateRecordDTO carGroupStockOperateRecordDTO);
}

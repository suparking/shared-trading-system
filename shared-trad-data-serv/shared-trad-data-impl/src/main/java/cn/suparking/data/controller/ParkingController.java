package cn.suparking.data.controller;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.data.service.CtpDataService;
import com.alibaba.fastjson.JSONObject;
import cn.suparking.data.api.beans.ParkConfigDTO;
import cn.suparking.data.service.ParkConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RefreshScope
@RestController
@RequestMapping("data-api")
public class ParkingController {

    private final CtpDataService ctpDataService;

    private final ParkConfigService parkConfigService;

    public ParkingController(final ParkConfigService parkConfigService, final CtpDataService ctpDataService) {
        this.parkConfigService = parkConfigService;
        this.ctpDataService = ctpDataService;
    }

    /**
     * 地锁复位时候先查询业务是否允许降板.
     * @param params {@link JSONObject}
     * @return {@link SpkCommonResult}
     */
    public SpkCommonResult searchBoardStatus(@RequestBody final JSONObject params) {
        return ctpDataService.searchBoardStatus(params);
    }

    /**
     * receive ctp park status.
     * @param params device data
     * @return {@link SpkCommonResult}
     */
    @PostMapping("/parkStatus")
    public SpkCommonResult parkStatus(@RequestBody final JSONObject params) {
        return ctpDataService.parkStatus(params);
    }

    /**
     * 项目配置信息变更通知.
     * @param parkSettingDTO {@link ParkConfigDTO}
     * @return {@link SpkCommonResult}
     */
    @PostMapping("/parkConfig")
    public SpkCommonResult parkingConfig(@Valid @RequestBody final ParkConfigDTO parkSettingDTO) {
        return parkConfigService.parkingConfig(parkSettingDTO);
    }
}

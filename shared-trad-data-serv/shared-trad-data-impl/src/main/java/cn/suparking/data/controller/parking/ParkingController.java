package cn.suparking.data.controller.parking;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.data.api.beans.ParkConfigDTO;
import cn.suparking.data.api.beans.ParkingLockModel;
import cn.suparking.data.api.beans.ProjectConfig;
import cn.suparking.data.api.parkfee.Parking;
import cn.suparking.data.api.query.ParkEventQuery;
import cn.suparking.data.api.query.ParkQuery;
import cn.suparking.data.api.query.ParkingQueryDTO;
import cn.suparking.data.dao.entity.ParkingDO;
import cn.suparking.data.dao.entity.ParkingEventDO;
import cn.suparking.data.dao.entity.ParkingTriggerDO;
import cn.suparking.data.service.CtpDataService;
import cn.suparking.data.service.ParkConfigService;
import cn.suparking.data.service.ParkingEventService;
import cn.suparking.data.service.ParkingService;
import cn.suparking.data.service.ParkingTriggerService;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.transaction.annotation.ShardingSphereTransactionType;
import org.apache.shardingsphere.transaction.core.TransactionType;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RefreshScope
@RestController
@RequestMapping("data-api")
public class ParkingController {

    private final CtpDataService ctpDataService;

    private final ParkingTriggerService parkingTriggerService;

    private final ParkingEventService parkingEventService;

    private final ParkConfigService parkConfigService;

    private final ParkingService parkingService;

    public ParkingController(final ParkConfigService parkConfigService, final CtpDataService ctpDataService,
                             final ParkingTriggerService parkingTriggerService, final ParkingEventService parkingEventService,
                             final ParkingService parkingService) {
        this.parkConfigService = parkConfigService;
        this.ctpDataService = ctpDataService;
        this.parkingTriggerService = parkingTriggerService;
        this.parkingEventService = parkingEventService;
        this.parkingService = parkingService;
    }

    /**
     * ???????????????????????????????????????????????????.
     *
     * @param params {@link JSONObject}
     * @return {@link SpkCommonResult}
     */
    public SpkCommonResult searchBoardStatus(@RequestBody final JSONObject params) {
        return ctpDataService.searchBoardStatus(params);
    }

    /**
     * ??????parking.
     * @param parking {@link Parking}
     * @return {@link Boolean}
     */
    @PostMapping("/parking")
    @ShardingSphereTransactionType(TransactionType.BASE)
    public Boolean createAndUpdateParking(@RequestBody final Parking parking) {
        return ctpDataService.createAndUpdateParking(parking);
    }

    /**
     * receive ctp park status.
     *
     * @param params device data
     * @return {@link SpkCommonResult}
     */
    @PostMapping("/parkStatus")
    public SpkCommonResult parkStatus(@RequestBody final JSONObject params) {
        return ctpDataService.parkStatus(params);
    }

    /**
     * ??????????????????????????????.
     *
     * @param parkSettingDTO {@link ParkConfigDTO}
     * @return {@link SpkCommonResult}
     */
    @PostMapping("/parkConfig")
    public SpkCommonResult parkingConfig(@Valid @RequestBody final ParkConfigDTO parkSettingDTO) {
        return parkConfigService.parkingConfig(parkSettingDTO);
    }

    /**
     * ??????deviceNo ?????? ???????????????.
     *
     * @param deviceNo device no.
     * @return {@link ParkingLockModel}
     */
    @GetMapping("/findParkingLock")
    public ParkingLockModel findParkingLock(@RequestParam("deviceNo") final String deviceNo) {
        return ctpDataService.findParkingLock(deviceNo);
    }

    /**
     * ??????????????????????????????.
     *
     * @param parkQuery {@link ParkQuery}
     * @return {@Link ParkingDO}
     */
    @PostMapping("/findParking")
    public ParkingDO findParking(@RequestBody final ParkQuery parkQuery) {
        return ctpDataService.findParking(parkQuery);
    }

    /**
     * ??????trigger id ?????? parking trigger event.
     *
     * @param triggerId parking trigger id.
     * @param projectId project id.
     * @return {@link ParkingTriggerDO}
     */
    @GetMapping("/findParkingTrigger")
    public ParkingTriggerDO findParkingTrigger(@RequestParam("projectId") final Long projectId, @RequestParam("triggerId") final Long triggerId) {
        return parkingTriggerService.findByProjectIdAndId(projectId, triggerId);
    }

    /**
     * ??????????????????,??????ID ??????????????????.
     *
     * @param parkEventQuery {@link ParkEventQuery}
     * @return {@link List}
     */
    @PostMapping("/findParkingEvents")
    public List<ParkingEventDO> findParkingEvents(@RequestBody final ParkEventQuery parkEventQuery) {
        return parkingEventService.findParkingEvents(parkEventQuery);
    }

    /**
     * ??????????????????????????????????????????.
     *
     * @param projectNo String
     * @return {@link ProjectConfig}
     */
    @GetMapping("/getProjectConfig")
    public ProjectConfig getProjectConfig(@RequestParam("projectNo") final String projectNo) {
        return ctpDataService.getProjectConfig(projectNo);
    }

    /**
     * ??????????????????????????????.
     *
     * @param parkingQueryDTO {@link ParkingQueryDTO}
     * @return {@link SpkCommonResult}
     */
    @PostMapping("/list")
    public SpkCommonResult list(@RequestBody ParkingQueryDTO parkingQueryDTO) {
        return parkingService.list(parkingQueryDTO);
    }

    /**
     * ??????????????????????????????.
     *
     * @return {@link SpkCommonResult}
     */
    @GetMapping("/findById")
    public SpkCommonResult findById(@RequestParam String id) {
        ParkingDO parkingDO = parkingService.findById(Long.valueOf(id));
        return SpkCommonResult.success(parkingDO);
    }

    /**
     * ??????????????????????????????.
     *
     * @return {@link SpkCommonResult}
     */
    @GetMapping("/findByPayParkingId")
    public SpkCommonResult findByPayParkingId(@RequestParam String payParkingId) {
        ParkingDO parkingDO = parkingService.findByPayParkingId(payParkingId);
        return SpkCommonResult.success(parkingDO);
    }
}

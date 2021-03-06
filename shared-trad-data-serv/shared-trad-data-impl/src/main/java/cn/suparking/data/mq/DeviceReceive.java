package cn.suparking.data.mq;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.data.Application;
import cn.suparking.data.api.beans.ParkStatusModel;
import cn.suparking.data.api.beans.ParkingLockModel;
import cn.suparking.data.api.beans.PublishData;
import cn.suparking.data.configuration.properties.MQConfigProperties;
import cn.suparking.data.mq.messageTemplate.DeviceMessageThread;
import cn.suparking.data.mq.messageTemplate.GroupInfoObj;
import cn.suparking.data.mq.messageTemplate.GroupInfoService;
import cn.suparking.data.mq.messageTemplate.MessageObj;
import cn.suparking.data.mq.messageTemplate.MessageTemplate;
import cn.suparking.data.mq.messageTemplate.RabbitUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static cn.suparking.data.api.constant.DataConstant.CTP_TYPE;
import static cn.suparking.data.api.constant.DataConstant.DATA_SAVE;

@Slf4j
@Component("DeviceReceive")
public class DeviceReceive implements MessageListener {

    @Resource
    private MQConfigProperties mqConfigProperties;

    private final RabbitUtils rabbitUtils;

    private final DeviceMessageThread deviceMessageThread = Application.getBean("DeviceMessageThread", DeviceMessageThread.class);

    public DeviceReceive(@Lazy final RabbitUtils rabbitUtils) {
        this.rabbitUtils = rabbitUtils;
    }

    @Override
    public void onMessage(final Message message) {
        try {
            log.info("Device MQ 接收到事件: " + message.toString());
            String from = (String) message.getMessageProperties().getHeaders().get("from");
            String topic = (String) message.getMessageProperties().getHeaders().get("topic");
            String body = new String(message.getBody());
            PublishData publishData = JSON.parseObject(body, PublishData.class);
            String lockCode = "";
            ParkStatusModel parkStatusModel = null;
            if (CTP_TYPE.equals(from) && topic.contains("device.status")) {
                if (Objects.nonNull(publishData) && publishData.getType().equals(DATA_SAVE)) {
                    parkStatusModel = JSON.parseObject(publishData.getData(), ParkStatusModel.class);
                    lockCode = parkStatusModel.getLockCode();
                } else {
                    log.warn("Device Receive CTP MQ Data Error Or Device Event Data: " + body);
                }
            } else {
                log.error(from + " 设备暂不支持, topic " + topic);
            }

            if (StringUtils.isNotBlank(lockCode)) {
                // 地锁编号不为空,并且可以在缓存中查询到此地锁,并且可以查到对应厂库编号
                ParkingLockModel parkingLockModel = deviceMessageThread.getParkInfoByDeviceNo(lockCode);
                if (Objects.nonNull(parkingLockModel) && StringUtils.isNotBlank(parkingLockModel.getProjectNo())) {
                    parkingLockModel.setParkId(parkingLockModel.getId());
                    MessageObj messageObj = GroupInfoService.findGroupInfoObj(parkingLockModel.getProjectNo(), mqConfigProperties.getQueueLength());
                    // 如果 返回不为null 那么就直接将消息存入队列
                    MessageTemplate parkingLockModelMessage = MessageTemplate.builder()
                            .parkingLockModel(parkingLockModel)
                            .parkStatusModel(parkStatusModel)
                            .message(message)
                            .build();
                    if (Optional.ofNullable(messageObj).isPresent()) {
                        // 将数据丢进 队列
                        GroupInfoService.setMessageInfo(parkingLockModelMessage, messageObj.getGroupId(), parkingLockModel.getProjectNo());
                        // 入队之后 判断 当前组的 线程是否开启,如果未开启 则开启
                        if (!GroupInfoService.getGroupThreadStatus(messageObj.getGroupId())) {
                            deviceMessageThread.deviceMessageHandler(messageObj.getGroupId());
                        }
                    } else {
                        // 未找到组信息
                        String groupId = buildGroupInfo(parkingLockModelMessage, parkingLockModel.getProjectNo());
                        // 开启 线程处理 事件 只需要将组ID 传递进去即可
                        deviceMessageThread.deviceMessageHandler(groupId);
                    }
                } else {
                    log.warn("来自: " + publishData.getFrom() + " 地锁,编号: " + lockCode + " : 经过核实,该编号不是平台设备,忽略执行以下逻辑.");
                }
            } else {
                rabbitUtils.sendRabbitAck(message, from, topic, (JSONObject) JSONObject.toJSON(SpkCommonResult.success("Device Receive Type Event, Not Deal.")));
            }

        } catch (Exception ex) {
            Arrays.stream(ex.getStackTrace()).forEach(item -> log.error(item.toString()));
        }
    }

    private String buildGroupInfo(final MessageTemplate message, final String projectNo) {
        GroupInfoObj groupInfoObj = GroupInfoObj.builder().build();
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        atomicBoolean.set(false);
        groupInfoObj.setThreadFlag(atomicBoolean);
        AtomicLong atomicLong = new AtomicLong();
        atomicLong.set(0L);
        groupInfoObj.setStartDealMillisecond(atomicLong);
        Vector<String> vector = new Vector<>(1);
        vector.add(projectNo);
        groupInfoObj.setProjectNos(vector);
        ConcurrentLinkedQueue<MessageTemplate> messages = new ConcurrentLinkedQueue<>();
        messages.offer(message);
        groupInfoObj.setMessages(messages);
        return GroupInfoService.insertGroupInfoObj(groupInfoObj);
    }
}

package cn.suparking.common.conf.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sparking.wx")
public class WxProperties {

    private String appid;

    private String secret;
}

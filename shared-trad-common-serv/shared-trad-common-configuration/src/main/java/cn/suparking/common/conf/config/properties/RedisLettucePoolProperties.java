package cn.suparking.common.conf.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.redis.lettuce.pool")
public class RedisLettucePoolProperties {
    private Integer maxActive;

    private Integer maxIdle;

    private Integer minIdle;

    private Integer maxWait;
}

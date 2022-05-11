package cn.suparking.user.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionKeyVO {
    //用户唯一标识
    @NotNull
    private String openid;

    //会话密钥
    private String sessionKey;

    //会话有效期, 以秒为单位, 例如2592000代表会话有效期为30天
    private String expiresIn;
}

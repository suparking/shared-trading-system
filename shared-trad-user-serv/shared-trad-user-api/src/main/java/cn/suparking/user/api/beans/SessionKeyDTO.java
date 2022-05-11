package cn.suparking.user.api.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionKeyDTO {
    //微信端返回code，使用code 换取 session_key
    @NotNull
    private String code;

    //用户信息的加密数据
    private String encryptedData;

    //对称解密算法初始向量
    private String iv;
}

package cn.suparking.user.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.suparking.common.api.exception.SpkCommonException;
import cn.suparking.user.api.beans.SessionKeyDTO;
import cn.suparking.user.api.vo.SessionKeyVO;
import cn.suparking.user.constant.UserConstant;
import cn.suparking.user.service.intf.UserLoginService;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserLoginServiceImpl implements UserLoginService {

    @Value("${wx.appid}")
    private String appid;

    @Value("${wx.secret}")
    private String secret;

    /**
     * 根据code获取openId和sessionKey.
     *
     * @param sessionKeyDTO {@linkplain SessionKeyDTO}
     * @return {@linkplain SessionKeyVO}
     */
    @Override
    public SessionKeyVO getSessionKey(SessionKeyDTO sessionKeyDTO) {
        SessionKeyVO sessionKeyVO = new SessionKeyVO();
        JSONObject params = new JSONObject();
        params.put("appid", appid);
        params.put("secret", secret);
        params.put("js_code", sessionKeyDTO.getCode());
        params.put("grant_type", UserConstant.GRANT_TYPE);

        try {
            log.info("code 换取 session_key ======> 请求参数 = [{}]", params.toJSONString());
            HttpResponse response = HttpRequest.post(UserConstant.JSCODE_TO_SESSION_URL).body(params.toJSONString()).timeout(2000).execute();
            String body = response.body();
            log.info("code 换取 session_key <====== 服务端返回 = [{}]", body);
            JSONObject ret_json = JSONObject.parseObject(body);
            String openid = ret_json.getString("openid");
            String sessionKey = ret_json.getString("session_key");
            String expiresIn = ret_json.getString("expires_in");
            if (StringUtils.isBlank(openid) || StringUtils.isBlank(sessionKey) || StringUtils.isBlank(expiresIn)) {
                throw new SpkCommonException("code 换取 session_key ======> 请求失败 [" + ret_json.toJSONString() + "]");
            }
            sessionKeyVO.setOpenid(openid);
            sessionKeyVO.setSessionKey(sessionKey);
            sessionKeyVO.setExpiresIn(expiresIn);
            return sessionKeyVO;
        } catch (Exception e) {
            log.error("请求code 换取 session_key异常 <====== error = [{}]", e.getMessage());
            throw new SpkCommonException("请求code 换取 session_key异常", e);
        }
    }
}

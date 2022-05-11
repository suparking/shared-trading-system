package cn.suparking.customer.controller.user.service.impl;


import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.customer.controller.user.service.UserOperateService;
import cn.suparking.customer.feign.user.UserTemplateService;
import cn.suparking.user.api.beans.SessionKeyDTO;
import cn.suparking.user.api.vo.SessionKeyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserOperateServiceImpl implements UserOperateService {

    @Autowired
    private UserTemplateService userTemplateService;

    /**
     * 一键登录.
     *
     * @param sessionKeyDTO {@linkplain SessionKeyDTO}
     * @return {@linkplain SpkCommonResult}
     */
    @Override
    public SpkCommonResult getSessionKey(SessionKeyDTO sessionKeyDTO) {
        //1.根据code获取openId和sessionKey
        SessionKeyVO sessionKey = userTemplateService.getSessionKey(sessionKeyDTO);

        //2.


        return SpkCommonResult.success(sessionKey);
    }
}

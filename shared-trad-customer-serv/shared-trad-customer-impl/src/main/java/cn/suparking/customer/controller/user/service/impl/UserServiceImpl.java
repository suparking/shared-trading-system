package cn.suparking.customer.controller.user.service.impl;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.customer.controller.user.service.UserService;
import cn.suparking.customer.feign.user.UserTemplateService;
import cn.suparking.user.api.beans.MiniLoginDTO;
import cn.suparking.user.api.beans.MiniRegisterDTO;
import cn.suparking.user.api.vo.PhoneInfoVO;
import cn.suparking.user.api.vo.SessionVO;
import cn.suparking.user.api.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserTemplateService userTemplateService;

    public UserServiceImpl(final UserTemplateService userTemplateService) {
        this.userTemplateService = userTemplateService;
    }

    /**
     * 一键登录.
     *
     * @param miniRegisterDTO {@linkplain MiniRegisterDTO}
     * @return {@linkplain SpkCommonResult}
     */
    @Override
    public UserVO register(final MiniRegisterDTO miniRegisterDTO) {
        UserVO userVO = null;
        //1.根据code获取openId和sessionKey
        SessionVO sessionVO = userTemplateService.getSessionKey(miniRegisterDTO.getCode());
        if (Objects.nonNull(sessionVO)) {
            // 解析手机号码,然后注册用户
            PhoneInfoVO phoneInfoVO = userTemplateService.getPhoneInfo(miniRegisterDTO.getPhoneCode());
            if (Objects.nonNull(phoneInfoVO)) {
            }
            return null;

        }
        // RegisterVO
        return userVO;
    }

    /**
     * mini user login.
     * @param code wx code
     * @return {@link UserVO}
     */
    @Override
    public UserVO login(final String code) {
        UserVO userVO = null;
        // 现根据 code 拿到用户信息
        SessionVO sessionVO = userTemplateService.getSessionKey(code);
        if (Objects.nonNull(sessionVO) && StringUtils.isNotBlank(sessionVO.getOpenid())) {
           // 拿着 openId 去查询用户信息返回给前端
            userVO = userTemplateService.getUserByOpenId(sessionVO.getOpenid());
        }
        return userVO;
    }

}

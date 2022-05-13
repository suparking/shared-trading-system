package cn.suparking.customer.controller.user.service;

import cn.suparking.user.api.beans.SessionKeyDTO;
import cn.suparking.user.api.vo.UserVO;

public interface UserOperateService {

    /**
     * get sessionkey.
     * @param sessionKeyDTO {@link SessionKeyDTO}
     * @return {@link UserVO}
     */
    UserVO getSessionKey(SessionKeyDTO sessionKeyDTO);
}

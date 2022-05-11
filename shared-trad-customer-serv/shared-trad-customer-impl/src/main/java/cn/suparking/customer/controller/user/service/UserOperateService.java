package cn.suparking.customer.controller.user.service;


import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.user.api.beans.SessionKeyDTO;

public interface UserOperateService {
    SpkCommonResult getSessionKey(SessionKeyDTO sessionKeyDTO);
}

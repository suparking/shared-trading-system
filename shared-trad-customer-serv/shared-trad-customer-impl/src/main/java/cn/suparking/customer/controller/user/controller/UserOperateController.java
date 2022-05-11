package cn.suparking.customer.controller.user.controller;

import cn.suparking.common.api.beans.SpkCommonResult;
import cn.suparking.customer.controller.user.service.UserOperateService;
import cn.suparking.user.api.beans.SessionKeyDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RefreshScope
@RestController
@RequestMapping("userOperate")
public class UserOperateController {

    @Autowired
    private UserOperateService userOperateService;

    @PostMapping("getSessionKey")
    public SpkCommonResult getSessionKey(@Valid @RequestBody SessionKeyDTO sessionKeyDTO) {
        return userOperateService.getSessionKey(sessionKeyDTO);
    }
}

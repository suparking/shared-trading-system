package cn.suparking.customer.feign.user;

import cn.suparking.user.api.beans.SessionKeyDTO;
import cn.suparking.user.api.vo.SessionKeyVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "shared-trad-user-serv", path = "/userLogin")
public interface UserTemplateService {

    @PostMapping("/getSessionKey")
    SessionKeyVO getSessionKey(@RequestBody SessionKeyDTO sessionKeyDTO);
}

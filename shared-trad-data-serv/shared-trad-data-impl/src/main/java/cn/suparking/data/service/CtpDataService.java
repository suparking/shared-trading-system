package cn.suparking.data.service;

import cn.suparking.common.api.beans.SpkCommonResult;
import com.alibaba.fastjson.JSONObject;

public interface CtpDataService {

    /**
     * ctp park status save.
     * @param obj {@Link JSONObject}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult parkStatus(JSONObject obj);
}

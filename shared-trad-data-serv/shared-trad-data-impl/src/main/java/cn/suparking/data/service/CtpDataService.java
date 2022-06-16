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

    /**
     * 地锁复位时候先查询业务是否允许降板.
     * @param params {@link JSONObject}
     * @return {@link SpkCommonResult}
     */
    SpkCommonResult searchBoardStatus(JSONObject params);
}

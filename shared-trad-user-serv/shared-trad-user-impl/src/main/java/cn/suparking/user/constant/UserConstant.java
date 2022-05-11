package cn.suparking.user.constant;

public class UserConstant {

    // ======================== 常量 ========================

    //code 换取 session_key 时固定grant_type值
    public static final String GRANT_TYPE = "authorization_code";


    // ======================== 路径 ========================

    //code 换取 session_key 接口地址
    public static final String JSCODE_TO_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session?appid=APPID&secret=SECRET&js_code=JSCODE&grant_type=authorization_code";
}

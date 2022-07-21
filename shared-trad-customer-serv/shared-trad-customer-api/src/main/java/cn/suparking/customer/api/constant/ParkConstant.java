package cn.suparking.customer.api.constant;

public class ParkConstant {

    public static final String SUCCESS = "00000";

    /**
     * 获取距离范围内的数据.
     */
    public static final String INTERFACE_NEARBYPARK = "/map/getCoordinate";

    /**
     * 获取所有场库信息.
     */
    public static final String INTERFACE_ALLPARK = "/map/getAllCoordinate";

    /**
     * 获取常去场库.
     */
    public static final String INTERFACE_REGULARPARK = "/map/regularLocations";

    /**
     * 根据设备编号获取场库信息.
     */
    public static final String INTERFACE_PARKBYDEVICE = "/map/projectInfoByDeviceNo";

    /**
     * 根据场库编号、协议id获取场库信息和合约协议信息.
     */
    public static final String INTERFACE_MYVIPCARINFO = "/vipGroup/myVipCarInfo";

    /**
     * 获取所有可线上办理的协议列表.
     */
    public static final String INTERFACE_NEWPROTOCOL = "/vipGroup/newProtocol";

    /**
     * 获取指定场库可线上办理的协议列表.
     */
    public static final String INTERFACE_NEWPROTOCOLBYPROJECTNO = "/vipGroup/newProtocolByProjectNo";

    /**
     * 获取所有可线上办理的场库.
     */
    public static final String INTERFACE_PROJECTLIST = "/vipGroup/projectListByNos";
}

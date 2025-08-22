package com.southMillion.webSocket_server.net;

public interface MsgIds {
    // Heartbeat
    int CS_HEARTBEAT_REQ  = 1053; // PB_CSHeartbeatReq
    int SC_HEARTBEAT_RESP = 1003; // PB_SCHeartbeatResp

    // Login (giữ nguyên nếu bạn đang dùng)
    int CS_LOGIN_REQ      = 7056;
    int SC_LOGIN_ACK      = 7000;

    // Time
    int CS_TIME_REQ       = 9050; // PB_CSTimeReq
    int SC_TIME_ACK       = 9000; // PB_SCTimeAck
    int SC_TIME_DATE_INFO = 9002; // PB_SCTimeDateInfo

    // Server control
    int SC_DISCONNECT_NOTICE = 9001; // PB_SCDisconnectNotice
    int SC_CROSS_CONNECT_INFO = 9003; // PB_SCCrossConnectInfo (chưa dùng ở đây)

    // ===== Knapsack (bag)
    int CS_KNAPSACK_REQ         = 1500;  // PB_CSKnapsackReq
    int SC_ITEM_NOT_ENOUGH      = 1504;  // PB_SCItemNotEnoughNotice
    int SC_KNAPSACK_ALL_INFO    = 1505;  // PB_SCKnapsackAllInfo
    int SC_KNAPSACK_SINGLE_INFO = 1506;  // PB_SCKnapsackSingleInfo
    int SC_GET_ITEM_NOTICE      = 1507;  // PB_SCGetItemNotice

    // ===== Equip
    int CS_EQUIP_REQ            = 1600;  // PB_CSEquipReq
    int SC_EQUIP_FUMO_LIST      = 1603;  // PB_SCEquipFuMoListInfo
    int SC_EQUIP_FUMO_ONE       = 1604;  // PB_SCEquipFuMoOneInfo
    int SC_EQUIP_LIST_INFO      = 1605;  // PB_SCEquipListInfo
    int SC_EQUIP_ONE_INFO       = 1606;  // PB_SCEquipOneInfo
    int SC_EQUIP_BAG_LIST_INFO  = 1607;  // PB_SCEquipBagListInfo
    int SC_EQUIP_BAG_ONE_INFO   = 1608;  // PB_SCEquipBagOneInfo

    // ===== Box
    int CS_BOX_REQ             = 1610;  // PB_CSBoxReq
    int SC_BOX_EQUIP_INFO      = 1615;  // PB_SCBoxEquipInfo
    int SC_BOX_INFO            = 1616;  // PB_SCBoxInfo
    int SC_BOX_SETING_INFO     = 1617;  // PB_SCBoxSetingInfo
    int SC_BOX_SELL_INFO       = 1618;  // PB_SCBoxSellInfo

    // ===== Shop (Market)
    int CS_SHOP_BUY_REQ        = 1620;  // PB_CSShopBuyReq
    int SC_SHOP_INFO           = 1621;  // PB_SCShopInfo
}

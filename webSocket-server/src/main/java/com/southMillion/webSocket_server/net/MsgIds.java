package com.southMillion.webSocket_server.net;

public interface MsgIds {

    // ===== Login
    int CS_LOGIN_REQ           = 7056; // PB_CSLoginToAccount
    int SC_LOGIN_ACK           = 7000; // PB_SCLoginToAccount
    int SC_ACCOUNT_KEY_ERR     = 7004; // PB_SCAccountKeyError

    // ===== Heartbeat / Time / Link
    int CS_HEARTBEAT_REQ       = 1053; // PB_CSHeartbeatReq
    int SC_HEARTBEAT_RESP      = 1003; // PB_SCHeartbeatResp

    int CS_TIME_REQ            = 9050; // PB_CSTimeReq
    int SC_TIME_ACK            = 9000; // PB_SCTimeAck
    int SC_DISCONNECT_NOTICE   = 9001; // PB_SCDisconnectNotice

    // ===== Role (info/attr/exp/level + WXInfo)
    int SC_ROLE_INFO_ACK       = 1400; // PB_SCRoleInfoAck
    int SC_ROLE_ATTR_LIST      = 1401; // PB_SCRoleAttrList
    int SC_ROLE_EXP_CHANGE     = 1402; // PB_SCRoleExpChange
    int SC_ROLE_LEVEL_CHANGE   = 1403; // PB_SCRoleLevelChange
    int CS_ROLE_WXINFO_SET     = 1405; // PB_CSRoleWXInfoSetReq

    // ===== Role / Settings (giữ theo client: 1460/1461)
    int CS_ROLE_SYSTEM_SET_REQ = 1460; // PB_CSRoleSystemSetReq
    int SC_ROLE_SYSTEM_SET_INFO= 1461; // PB_SCRoleSystemSetInfo

    // ===== Other Role
    int CS_GET_OTHER_ROLE_INFO = 1462; // PB_CSGetOtherRoleInfo
    int SC_GET_OTHER_ROLE_RET  = 1463; // PB_SCGetOtherRoleRet

    // ===== Mail (giữ theo client: 9551/950x)
    int CS_MAIL_REQ            = 9551; // PB_CSMailReq
    int SC_MAIL_DELETE_ACK     = 9501; // PB_SCMailDeleteAck
    int SC_MAIL_LIST_ACK       = 9504; // PB_SCMailListAck
    int SC_MAIL_DETAIL         = 9505; // PB_SCMailDetail
    int SC_FETCH_MAIL_ACK      = 9506; // PB_SCFetchMailAck

    // ===== Advertisement (giữ theo client: 1663/1662)
    int CS_ADVERTISEMENT_FETCH = 1663; // PB_CSAdvertisementFetch
    int SC_ADVERTISEMENT_INFO  = 1662; // PB_SCAdvertisementInfo


    // ===== Bag / Knapsack (theo proto)
    int CS_KNAPSACK_REQ              = 1500; // PB_CSKnapsackReq
    int CS_BUY_CMD_REQ               = 1501; // PB_CSBuyCmdReq

    int SC_ITEM_NOT_ENOUGH_NOTICE    = 1504; // PB_SCItemNotEnoughNotice
    int SC_KNAPSACK_ALL_INFO         = 1505; // PB_SCKnapsackAllInfo
    int SC_KNAPSACK_SINGLE_INFO      = 1506; // PB_SCKnapsackSingleInfo
    int SC_GET_ITEM_NOTICE           = 1507; // PB_SCGetItemNotice
    int SC_GET_ONE_ITEM_NOTICE       = 1508; // PB_SCGetOneItemNotice

    int SC_ALL_SHIZHUANG_INFO        = 1509; // PB_SCAllShiZhuangInfo
    int SC_SHIZHUANG_INFO            = 1510; // PB_SCShiZhuangInfo
}
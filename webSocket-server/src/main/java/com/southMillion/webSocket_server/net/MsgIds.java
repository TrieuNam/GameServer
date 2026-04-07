package com.SouthMillion.webSocket_server.net;

public interface MsgIds {

    // ===== Box
    int SC_BOX_EQUIP_COMPARE_INFO     = 1619; // PB_SCBoxEquipCompareInfo

    // ===== System / Notice
    int SC_NOTICE_NUM              = 700;  // PB_SCNoticeNum — notice number update

    // ===== Login / Enter GS
    int CS_LOGIN_REQ               = 7056; // PB_CSLoginToAccount
    int SC_LOGIN_ACK               = 7000; // PB_SCLoginToAccount
    int SC_ACCOUNT_KEY_ERR         = 7004; // PB_SCAccountKeyError
    int SC_USER_ENTER_GS_ACK       = 1000; // PB_SCUserEnterGSAck
    int SC_SERVER_BUSY             = 1002; // PB_SCServerBusy
    int CS_USER_LOGOUT             = 1051; // PB_CSUserLogout

    // ===== Heartbeat / Time / Link
    int CS_HEARTBEAT_REQ           = 1053; // PB_CSHeartbeatReq
    int SC_HEARTBEAT_RESP          = 1003; // PB_SCHeartbeatResp

    int CS_TIME_REQ                = 9050; // PB_CSTimeReq
    int SC_TIME_ACK                = 9000; // PB_SCTimeAck
    int SC_DISCONNECT_NOTICE       = 9001; // PB_SCDisconnectNotice

    // ===== Role (info/attr/exp/level + WXInfo)
    int CS_ALL_INFO_REQ            = 1450; // PB_CSAllInfoReq — request all data push on login
    int SC_ROLE_INFO_ACK           = 1400; // PB_SCRoleInfoAck
    int SC_ROLE_ATTR_LIST          = 1401; // PB_SCRoleAttrList
    int SC_ROLE_EXP_CHANGE         = 1402; // PB_SCRoleExpChange
    int SC_ROLE_LEVEL_CHANGE       = 1403; // PB_SCRoleLevelChange
    int SC_ATTR_LIST_REASON        = 1404; // PB_SCAttrListReason
    int CS_ROLE_WXINFO_SET         = 1405; // PB_CSRoleWXInfoSetReq

    // ===== Task reward (msgrole.proto: 1451-1452)
    int CS_FETCH_TASK_REWARD_REQ   = 1451; // PB_CSFetchTaskRewardReq
    int SC_TASK_PROGRESS_INFO      = 1452; // PB_SCTaskProgressInfo

    // ===== Lazy load feature data (msgrole.proto: 1453)
    int CS_FEATURE_DATA_REQ        = 1453; // PB_CSFeatureDataReq — request lazy-loaded module data

    // ===== Role / Settings (giữ theo client: 1460/1461)
    int CS_ROLE_SYSTEM_SET_REQ     = 1460; // PB_CSRoleSystemSetReq
    int SC_ROLE_SYSTEM_SET_INFO    = 1461; // PB_SCRoleSystemSetInfo

    // ===== Other Role
    int CS_GET_OTHER_ROLE_INFO     = 1462; // PB_CSGetOtherRoleInfo
    int SC_GET_OTHER_ROLE_RET      = 1463; // PB_SCGetOtherRoleRet

    // ===== Notice Time (msgrole.proto / msgother.hpp)
    int CS_NOTICE_TIME_REQ         = 1464; // PB_CSNoticeTimeReq
    int SC_NOTICE_TIME_RET         = 1465; // PB_SCNoticeTimeRet

    // ===== Server command to client
    int SC_CMD_TO_CLIENT_CMD       = 1466; // PB_SCCmdToClientCmd

    // ===== Limit Core / Breakthrough (msgother.hpp: 1467-1468)
    int CS_LIMIT_CORE_REQ          = 1467; // PB_CSLimitCoreReq
    int SC_LIMIT_CORE_INFO         = 1468; // PB_SCLimitCoreInfo

    // ===== Mail (giữ theo client: 9551/950x)
    int CS_MAIL_REQ                = 9551; // PB_CSMailReq
    int SC_MAIL_DELETE_ACK         = 9501; // PB_SCMailDeleteAck
    int SC_MAIL_LIST_ACK           = 9504; // PB_SCMailListAck
    int SC_MAIL_DETAIL             = 9505; // PB_SCMailDetail
    int SC_FETCH_MAIL_ACK          = 9506; // PB_SCFetchMailAck

    // ===== Advertisement (giữ theo client: 1663/1662)
    int CS_ADVERTISEMENT_FETCH     = 1663; // PB_CSAdvertisementFetch
    int SC_ADVERTISEMENT_INFO      = 1662; // PB_SCAdvertisementInfo


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

    // ===== Crafting
    int CS_CRAFT_REQ                 = 1700; // PB_CSCraftReq
    int SC_CRAFT_INFO                = 1701; // PB_SCCraftInfo
    int SC_CRAFT_START               = 1702; // PB_SCCraftStart
    int SC_CRAFT_STATUS              = 1703; // PB_SCCraftStatus
    int SC_CRAFT_CLAIM               = 1704; // PB_SCCraftClaim

    // ===== Chat
    int CS_CHAT_REQ                  = 1800; // PB_CSChatReq
    int SC_CHAT_SEND                 = 1801; // PB_SCChatSend
    int SC_CHAT_HISTORY              = 1802; // PB_SCChatHistory
    int SC_CHAT_MUTE                 = 1803; // PB_SCChatMute

    // ===== Friend
    int CS_FRIEND_REQ                = 1900; // PB_CSFriendReq
    int SC_FRIEND_LIST               = 1901; // PB_SCFriendList
    int SC_FRIEND_OPERATION          = 1902; // PB_SCFriendOperation
    int SC_FRIEND_REQUEST            = 1903; // PB_SCFriendRequest
    int SC_FRIEND_SEARCH             = 1904; // PB_SCFriendSearch
    int SC_FRIEND_ONLINE             = 1905; // PB_SCFriendOnline

    // ===== Rank / Leaderboard (msgrank.proto: 9601-9602)
    int SC_RANK_LIST                 = 9601; // PB_SCRankList
    int CS_RANK_REQ                  = 9602; // PB_CSRankReq

    // ===== Guild (theo proto gốc msgguild.proto + MsgIdManger.ts)
    int CS_GUILD_REQ                 = 9640; // PB_CSGuildReq        (was 2000 — FIXED)
    int SC_GUILD_SEARCH_LIST         = 9641; // PB_SCGuildSearchList (NEW)
    int SC_GUILD_INFO                = 9642; // PB_SCGuildInfo       (was 2001 — FIXED)
    int SC_GUILD_REPORT_LIST         = 9643; // PB_SCGuildReportList (NEW)
    int SC_GUILD_MEMBER_LIST         = 9644; // PB_SCGuildMemberList (NEW)
    int SC_GUILD_APP_LIST            = 9645; // PB_SCGuildAppList    (NEW)
    int SC_GUILD_ROLE_INFO           = 9646; // PB_SCGuildRoleInfo   (NEW)

    // ===== Arena (theo proto gốc msgarena.proto + MsgIdManger.ts)
    int CS_ARENA_REQ                 = 9610; // PB_CSArenaReq            (was 2300 — FIXED)
    int SC_ARENA_INFO                = 9611; // PB_SCArenaInfo            (was 2301 — FIXED)
    int SC_ARENA_REPORT_LIST         = 9612; // PB_SCArenaReportList      (was 2302 — FIXED)
    int CS_CROSS_ARENA_REQ           = 9613; // PB_CSCrossArenaReq        (NEW)
    int SC_CROSS_ARENA_INFO          = 9614; // PB_SCCrossArenaInfo       (NEW)
    int SC_CROSS_ARENA_REPORT_LIST   = 9615; // PB_SCCrossArenaReportList (NEW)
    int SC_CROSS_ARENA_FIGHT_RET     = 9616; // PB_SCCrossArenaFightRet   (NEW)

    // ===== Formation / Trận Pháp (msgbattle.proto: 8144-8147)
    int CS_BATTLE_FORMATION_REQ      = 8144; // JSON op packet: op=1 save slots, op=2 level-up
    int SC_BATTLE_FORMATION_ACK      = 8145; // Formation save/level-up ACK
    int CS_FORMATION_QUERY_REQ       = 8146; // Query current formation state
    int SC_FORMATION_QUERY_ACK       = 8147; // Formation state response

    // ===== Battle / Combat (WebSocket -> battleserver-service)
    int CS_BATTLE_REQ                = 9650; // JSON or binary op packet
    int SC_BATTLE_RESP               = 9651; // Unified battle response

    // ===== Angel (msgangel.proto: 2130-2132)
    int CS_ANGEL_REQ                 = 2130; // PB_CSAngelReq
    int SC_ANGEL_INFO                = 2131; // PB_SCAngelInfo
    int SC_ANGEL_RET                 = 2132; // PB_SCAngelOpRet

    // ===== Mount (msgmount.proto: 2140-2145)
    int CS_MOUNT_REQ                 = 2140; // PB_CSMountReq
    int SC_MOUNT_INFO                = 2141; // PB_SCMountInfo
    int SC_MOUNT_OP_RET              = 2142; // PB_SCMountOpRet
    int SC_MOUNT_HARNESS_LIST_INFO   = 2143; // PB_SCMountHarnessListInfo
    int SC_MOUNT_HARNESS_ONE_INFO    = 2144; // PB_SCMountHarnessOneInfo — single harness update
    int SC_MOUNT_HARNESS_INFO        = 2145; // PB_SCMountHarnessInfo

    // ===== Skill (msgskill.proto: 1470-1481)
    int CS_ROLE_SKILL_OPERA_REQ      = 1470; // PB_CSRoleSkillOperaReq  (0=info, 1=learn, 2=one-key-up)
    int SC_ROLE_SKILL_ALL_INFO       = 1471; // PB_SCRoleSkillAllInfo
    int CS_ROLE_TALENT_OPERA_REQ     = 1480; // PB_CSRoleTalentOperaReq (0=info, 1=learn-talent)
    int SC_ROLE_TALENT_ALL_INFO      = 1481; // PB_SCRoleTalentAllInfo

    // ===== Random Activity (msgrandactivity.proto / msgopenserveractivity.proto: 3000-3042)
    int CS_RAND_ACTIVITY_OPERA_REQ   = 3000; // PB_CSRandActivityOperaReq
    int SC_CHONG_ZHI_INFO            = 3001; // PB_SCChongZhiInfo (recharge history)
    int SC_CHONG_ZHI_INFO_CHANGE     = 3002; // PB_SCChongZhiInfoChange
    int SC_ACTIVITY_STATUS           = 3003; // PB_SCActivityStatus (generic ack)
    int CS_CHONG_ZHI_CONFIG_REQ      = 3004; // PB_CSChongZhiConfigReq
    int SC_CHONG_ZHI_CONFIG_INFO     = 3005; // PB_SCChongZhiConfigInfo

    // ===== Analytics (JSON-based, 9200-9203)
    int CS_ANALYTICS_REQ             = 9200; // client → server: op 1=trackEvent, 2=getEvents, 3=getKpi
    int SC_ANALYTICS_TRACK_ACK       = 9201; // server → client: ACK sau khi track event thành công
    int SC_ANALYTICS_EVENTS          = 9202; // server → client: danh sách events của player
    int SC_ANALYTICS_KPI             = 9203; // server → client: KPI data của player
}
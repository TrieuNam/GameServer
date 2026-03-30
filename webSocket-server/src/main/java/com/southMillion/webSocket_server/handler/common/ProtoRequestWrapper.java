package com.SouthMillion.webSocket_server.handler.common;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Generic wrapper for proto request messages that follow the pattern:
 * - req_type/operation field
 * - param repeated field  
 * - str_param bytes field
 * 
 * Provides convenient accessors to avoid dealing with param indices
 */
public class ProtoRequestWrapper {
    private final MessageLite message;
    private final Integer reqType;
    private final List<Integer> params;
    private final String strParam;
    
    public ProtoRequestWrapper(MessageLite message) {
        this.message = message;
        this.reqType = extractReqType();
        this.params = extractParams();
        this.strParam = extractStrParam();
    }
    
    private Integer extractReqType() {
        try {
            // Try getReqType() first
            Method method = message.getClass().getMethod("getReqType");
            return (Integer) method.invoke(message);
        } catch (Exception e) {
            // Try getOp() as fallback
            try {
                Method method = message.getClass().getMethod("getOp");
                return (Integer) method.invoke(message);
            } catch (Exception ex) {
                return 0;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<Integer> extractParams() {
        try {
            Method method = message.getClass().getMethod("getParamList");
            return (List<Integer>) method.invoke(message);
        } catch (Exception e) {
            return List.of();
        }
    }
    
    private String extractStrParam() {
        try {
            Method hasMethod = message.getClass().getMethod("hasStrParam");
            boolean has = (Boolean) hasMethod.invoke(message);
            if (!has) return "";
            
            Method getMethod = message.getClass().getMethod("getStrParam");
            ByteString bs = (ByteString) getMethod.invoke(message);
            return bs.toStringUtf8();
        } catch (Exception e) {
            return "";
        }
    }
    
    // Convenient accessors
    public int getOp() {
        return reqType != null ? reqType : 0;
    }
    
    public int getReqType() {
        return getOp();
    }
    
    public int getParam(int index) {
        if (params == null || index >= params.size()) return 0;
        return params.get(index);
    }
    
    public int getParamCount() {
        return params != null ? params.size() : 0;
    }
    
    public String getStrParam() {
        return strParam != null ? strParam : "";
    }
    
    // Semantic accessors (map param indices to meaningful names)
    public int getGuildId() { return getParam(0); }
    public int getTargetRoleId() { return getParam(0); }
    public int getDonationType() { return getParam(0); }
    public int getAmount() { return getParam(1); }
    public int getTechBranch() { return getParam(0); }
    public int getShipId() { return getParam(0); }
    public int getRouteType() { return getParam(1); }
    public int getRankType() { return getParam(0); }
    public int getConstellationId() { return getParam(0); }
    public int getStarIndex() { return getParam(1); }
    public int getBirthMonth() { return getParam(0); }
    public int getBirthDay() { return getParam(1); }
    public int getBuildingType() { return getParam(0); }
    public int getSlotId() { return getParam(1); }
    public int getBuildingId() { return getParam(0); }
    public int getRuneId() { return getParam(0); }
    public int getSlotIndex() { return getParam(1); }
    public List<Integer> getRuneIdsList() { return params; }
    public int getScrollId() { return getParam(0); }
    public List<Integer> getScrollIdsList() { return params; }
    public int getSetId() { return getParam(0); }
    public int getArtifactId() { return getParam(0); }
    public int getEvolutionPath() { return getParam(1); }
    public int getSpiritId() { return getParam(1); }
    public int getDrawType() { return getParam(0); }
    public int getDrawCount() { return getParam(1); }
    
    public String getGuildName() { return getStrParam(); }
    public String getKeyword() { return getStrParam(); }
    public String getNotice() { return getStrParam(); }
    public int getTechId() { return getParam(2); }
    public boolean getApproved() { return getParam(2) == 1; }
}

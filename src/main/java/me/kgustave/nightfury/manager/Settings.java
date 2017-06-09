/*
 * Copyright 2017 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.kgustave.nightfury.manager;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This is a wrapper for JSONs holding server-specific data which can be retrieved via the NightFuryManager.
 * 
 * @author Kaidan Gustave
 */
public class Settings {
    
    private final NightFuryManager manager;
    private final long id;
    private final JSONObject guildJSON;
    
    private static final String PREFIXES = "prefix";
    private static final String GUILD_ID = "guild_id";
    private static final String MOD_ROLE_ID = "mod_role_id";
    private static final String MOD_LOG_ID = "mod_log_id";
    private static final String MUTED_ROLE_ID = "muted_role_id";
    private static final String MUTED_USERS = "muted_users";
    private static final String CASE_NUMBER = "case_number";
    private static final String CASES = "cases";
    private static final String ROLEME_ROLE_IDS = "roleme_role_ids";
    private static final String COLORME_ROLE_IDS = "colorme_role_ids";
    
    public Settings(NightFuryManager manager, long id, JSONObject guildJSON) {
        this.manager = manager;
        this.id = id;
        if(!guildJSON.has(PREFIXES))
            guildJSON.put(PREFIXES, new JSONArray().put("|"));
        if(!guildJSON.has(GUILD_ID))
            guildJSON.put(GUILD_ID, id);
        if(!guildJSON.has(MOD_ROLE_ID))
            guildJSON.put(MOD_ROLE_ID, 0L);
        if(!guildJSON.has(MOD_LOG_ID))
            guildJSON.put(MOD_LOG_ID, 0L);
        if(!guildJSON.has(MUTED_ROLE_ID))
            guildJSON.put(MUTED_ROLE_ID, 0L);
        if(!guildJSON.has(MUTED_USERS))
            guildJSON.put(MUTED_USERS, new JSONArray());
        if(!guildJSON.has(CASE_NUMBER))
            guildJSON.put(CASE_NUMBER, 0);
        if(!guildJSON.has(CASES))
            guildJSON.put(CASES, new JSONObject());
        if(!guildJSON.has(ROLEME_ROLE_IDS))
            guildJSON.put(ROLEME_ROLE_IDS, new JSONArray());
        if(!guildJSON.has(COLORME_ROLE_IDS))
            guildJSON.put(COLORME_ROLE_IDS, new JSONArray());
        this.guildJSON = guildJSON;
    }
    
    
    // CALLBACK METHODS
    public NightFuryManager getManager() {
        return manager;
    }
    
    public void save() {
        manager.put(this);
    }
    
    
    // GENERAL METHODS
    public String getId() {
        return String.valueOf(id);
    }
    
    public long getIdLong() {
        return id;
    }
    
    public JSONObject getGuildJSON() {
        return guildJSON;
    }
    
    public String printSettings() {
        StringBuilder builder = new StringBuilder("{\n");
        String indent = "    ";
        builder.append(indent).append(PREFIXES).append(" : ").append(guildJSON.getJSONArray(PREFIXES).toString()).append("\n");
        builder.append(indent).append(MOD_ROLE_ID).append(" : ").append(hasModId() ? getModId() : "NONE").append("\n");
        builder.append(indent).append(MOD_LOG_ID).append(" : ").append(hasModLogId() ? getModLogId() : "NONE").append("\n");
        builder.append(indent).append(MUTED_ROLE_ID).append(" : ").append(hasMutedId() ? getMutedId() : "NONE").append("\n");
        builder.append(indent).append(CASE_NUMBER).append(" : ").append(getCurrentCaseNumber()).append("\n");
        builder.append(indent).append(CASES).append(" : ").append("\n").append(indent).append("{").append("\n");
        if(guildJSON.getJSONObject(CASES).length()!=0) {
            JSONObject cases = guildJSON.getJSONObject(CASES);
            cases.keySet().forEach((key) -> {
                builder.append(indent).append(indent).append(key).append(" : ").append("\n").append(indent).append(indent).append("{").append("\n");
                Case c = new Case(cases.getJSONObject(key));
                builder.append(indent).append(indent).append(indent).append("mod_id : " + c.modId + "\n");
                builder.append(indent).append(indent).append(indent).append("message_id : " + c.messageId + "\n");
                builder.append(indent).append(indent).append(indent).append("reason : " + (c.reason.equals("null")?"NONE":c.reason) + "\n");
                builder.append(indent).append(indent).append("}").append("\n");
            });
        } else {
            builder.append(indent).append(indent).append("NONE").append("\n");
        }
        builder.append(indent).append("}").append("\n");
        return builder.append("}").toString();
    }
    
    
    // CONFIGURATIONS METHODS
    public JSONArray getPrefixes() {
        return guildJSON.getJSONArray(PREFIXES);
    }
    
    public boolean addPrefix(String prefix) {
        JSONArray prefixes = getPrefixes();
        for(int i=0; i<prefixes.length(); i++)
        {
            if(prefixes.optString(i).equalsIgnoreCase(prefix)) {
                return false;
            }
        }
        setPrefixes(prefixes.put(prefix));
        return true;
    }
    
    public boolean removePrefix(String prefix) {
        JSONArray prefixes = getPrefixes();
        for(int i=0; i<prefixes.length(); i++)
        {
            if(prefixes.optString(i).equalsIgnoreCase(prefix)) {
                prefixes.remove(i);
                setPrefixes(prefixes);
                return true;
            }
        }
        return false;
    }
    
    public void setPrefixes(JSONArray prefixes) {
        guildJSON.put(PREFIXES, prefixes);
        manager.put(this);
    }
    
    public Settings putPrefixes(JSONArray prefixes) {
        guildJSON.put(PREFIXES, prefixes);
        return this;
    }
    
    
    // MODERATOR ROLE ID
    public boolean hasModId() {
        return getModId() != 0L;
    }
    
    public long getModId() {
        return guildJSON.getLong(MOD_ROLE_ID);
    }
    
    public void setModId(long modId) {
        guildJSON.put(MOD_ROLE_ID, modId);
        manager.put(this);
    }
    
    public Settings putModId(long modId) {
        guildJSON.put(MOD_ROLE_ID, modId);
        return this;
    }
    
    
    // MODERATOR LOG ID
    public boolean hasModLogId() {
        return getModLogId() != 0L;
    }
    
    public long getModLogId() {
        return guildJSON.getLong(MOD_LOG_ID);
    }
    
    public void setModLogId(long modLogId) {
        guildJSON.put(MOD_LOG_ID, modLogId);
        manager.put(this);
    }
    
    public Settings putModLogId(long modLogId) {
        guildJSON.put(MOD_LOG_ID, modLogId);
        return this;
    }
    
    
    // MUTED ROLE ID
    public boolean hasMutedId() {
        return getMutedId() != 0L;
    }
    
    public long getMutedId() {
        return guildJSON.getLong(MUTED_ROLE_ID);
    }
    
    public void setMutedId(long mutedId) {
        guildJSON.put(MUTED_ROLE_ID, mutedId);
        manager.put(this);
    }
    
    public Settings putMutedId(long mutedId) {
        guildJSON.put(MUTED_ROLE_ID, mutedId);
        return this;
    }
    
    
    // MUTED USERS
    public JSONArray getMutedArray()
    {
        return guildJSON.getJSONArray(MUTED_USERS);
    }
    
    public int numberMuted()
    {
        return getMutedArray().length();
    }
    
    public boolean isMuted(long userId)
    {
        JSONArray array = getMutedArray();
        if(array.length()==0)
            return false;
        for(int i=0; i<array.length(); i++) {
            if(array.getLong(i)==userId)
                return true;
        }
        return false;
    }
    
    public boolean addMute(long userId)
    {
        if(isMuted(userId))
            return false;
        guildJSON.put(MUTED_USERS, getMutedArray().put(userId));
        manager.put(this);
        return true;
    }
    
    public Settings putMute(long userId)
    {
        if(isMuted(userId))
            throw new IllegalArgumentException("Provided User ID was already muted!");
        guildJSON.put(MUTED_USERS, getMutedArray().put(userId));
        return this;
    }
    
    public boolean removeMute(long userId)
    {
        if(!isMuted(userId))
            return false;
        JSONArray array = getMutedArray();
        for(int i=0; i<array.length(); i++) {
            if(array.getLong(i)==userId)
                array.remove(i);
        }
        guildJSON.put(MUTED_USERS, array);
        manager.put(this);
        return true;
    }
    
    public void addMutes(long... userIds)
    {
        JSONArray array = getMutedArray();
        for(long userId : userIds) {
            if(!isMuted(userId)) {
                array.put(userId);
            }
        }
        guildJSON.put(MUTED_USERS, array);
        manager.put(this);
    }
    
    public void putClearMutes()
    {
        guildJSON.put(MUTED_USERS, new JSONArray());
    }
    
    public void clearMutes()
    {
        guildJSON.put(MUTED_USERS, new JSONArray());
        manager.put(this);
    }
    
    
    // CASE NUMBER
    public int getCurrentCaseNumber() {
        return guildJSON.getInt(CASE_NUMBER);
    }
    
    public int getNextCaseNumber() {
        return 1+getCurrentCaseNumber();
    }
    
    
    // CASES
    public JSONObject getCasesJSON()
    {
        return guildJSON.getJSONObject(CASES);
    }
    
    public void addCase(long modId, long messageId)
    {
        guildJSON.put(CASES, guildJSON.getJSONObject(CASES).put(String.valueOf(getNextCaseNumber()), new Case(modId, messageId, null).toJSON()));
        guildJSON.put(CASE_NUMBER, getNextCaseNumber());
        manager.put(this);
    }
    
    public void addCase(long modId, long messageId, String reason)
    {
        guildJSON.put(CASES, guildJSON.getJSONObject(CASES).put(String.valueOf(getNextCaseNumber()), new Case(modId, messageId, reason).toJSON()));
        guildJSON.put(CASE_NUMBER, getNextCaseNumber());
        manager.put(this);
    }
    
    public boolean addReasonToCase(String reason, int caseNumber)
    {
        JSONObject cases = guildJSON.getJSONObject(CASES);
        if(cases.length()==0 || caseNumber>getCurrentCaseNumber() || !cases.has(String.valueOf(caseNumber)))
            return false;
        Case c = Case.fromCasesJSON(cases, caseNumber);
        guildJSON.put(CASES, cases.put(String.valueOf(caseNumber), c.reason(reason).toJSON()));
        manager.put(this);
        return true;
    }
    
    public String getReasonForCase(int caseNumber)
    {
        Case c = Case.fromCasesJSON(guildJSON.getJSONObject(CASES), caseNumber);
        if(c==null)
            return null;
        return c.reason;
    }
    
    public long getMessageIdForCase(int caseNumber)
    {
        Case c = Case.fromCasesJSON(guildJSON.getJSONObject(CASES), caseNumber);
        if(c==null)
            return 0L;
        return c.messageId;
    }
    
    public long getModIdForCase(int caseNumber)
    {
        Case c = Case.fromCasesJSON(guildJSON.getJSONObject(CASES), caseNumber);
        if(c==null)
            return 0L;
        return c.modId;
    }
    
    public int getNewestCaseForId(long modId)
    {
        JSONObject cases = guildJSON.getJSONObject(CASES);
        Object[] userCases = cases.keySet().stream().filter(key -> {
            Case c = new Case(cases.getJSONObject(key));
            return c.modId==modId && c.reason.equals("null");
        }).toArray();
        if(userCases.length==0)
            return 0;
        if(userCases.length==1)
            return Integer.parseInt((String)userCases[0]);
        int prev=Integer.parseInt((String)userCases[0]);
        for(int i=1; i<userCases.length; i++) {
            int curr = Integer.parseInt((String)userCases[i]);
            prev = (prev<curr? curr : prev);
        }
        return prev;
    }
    
    public void resetCaseSystem()
    {
        guildJSON.put(CASE_NUMBER, 0);
        guildJSON.put(CASES, new JSONObject());
        manager.put(this);
    }
    
    public Settings putNewCaseSystem()
    {
        guildJSON.put(CASE_NUMBER, 0);
        guildJSON.put(CASES, new JSONObject());
        return this;
    }
    
    // Why do I have a private class for wrapping cases?
    // Simply put: it saves space and makes sure I don't mistype a JSON key or make some other small error
    private static class Case {
        
        public final long modId;
        public final long messageId;
        public String reason;
        
        public static Case fromCasesJSON(JSONObject json, int number) {
            if(json.length()<number)
                return null;
            return new Case(json.getJSONObject(String.valueOf(number)));
        }
        
        public Case(JSONObject caseJSON)
        {
            this.modId = caseJSON.optLong("mod_id");
            this.messageId = caseJSON.optLong("message_id");
            this.reason = caseJSON.optString("reason");
            if(modId==0 || messageId==0)
                throw new IllegalStateException("JSONObject provided was had one or more invalid fields");
        }
        
        public Case(long modId, long messageId, String reason)
        {
            this.modId = modId;
            this.messageId = messageId;
            this.reason = reason;
        }
        
        public Case reason(String reason)
        {
            this.reason = reason;
            return this;
        }
        
        public JSONObject toJSON() {
            return new JSONObject().put("mod_id", modId).put("message_id", messageId).put("reason", reason==null? "null" : reason);
        }
        
    }
    
    
    // ROLEME ROLES
    public JSONArray getRoleMeArray()
    {
        return guildJSON.getJSONArray(ROLEME_ROLE_IDS);
    }
    
    public boolean isRoleMe(long roleId)
    {
        JSONArray array = getRoleMeArray();
        if(array.length()==0)
            return false;
        for(int i=0; i<array.length(); i++) {
            if(array.getLong(i)==roleId)
                return true;
        }
        return false;
    }
    
    public boolean addRoleMe(long roleId)
    {
        if(isRoleMe(roleId))
            return false;
        guildJSON.put(ROLEME_ROLE_IDS, getRoleMeArray().put(roleId));
        manager.put(this);
        return true;
    }
    
    public Settings putRoleme(long roleId)
    {
        if(isRoleMe(roleId))
            throw new IllegalArgumentException("Provided User ID was already muted!");
        guildJSON.put(ROLEME_ROLE_IDS, getRoleMeArray().put(roleId));
        return this;
    }
    
    public boolean removeRoleMe(long roleId)
    {
        if(!isRoleMe(roleId))
            return false;
        JSONArray array = getRoleMeArray();
        for(int i=0; i<array.length(); i++) {
            if(array.getLong(i)==roleId)
                array.remove(i);
        }
        guildJSON.put(ROLEME_ROLE_IDS, array);
        manager.put(this);
        return true;
    }
    
    public void putClearRoleMes()
    {
        guildJSON.put(ROLEME_ROLE_IDS, new JSONArray());
    }
    
    public void clearRoleMes()
    {
        guildJSON.put(ROLEME_ROLE_IDS, new JSONArray());
        manager.put(this);
    }
    
    
    // COLORME ROLES
    public JSONArray getColormeArray()
    {
        return guildJSON.getJSONArray(COLORME_ROLE_IDS);
    }
    
    public boolean isColorme(long roleId)
    {
        JSONArray array = getColormeArray();
        if(array.length()==0)
            return false;
        for(int i=0; i<array.length(); i++) {
            if(array.getLong(i)==roleId)
                return true;
        }
        return false;
    }
    
    public boolean addColorme(long roleId)
    {
        if(isColorme(roleId))
            return false;
        guildJSON.put(COLORME_ROLE_IDS, getColormeArray().put(roleId));
        manager.put(this);
        return true;
    }
    
    public Settings putColorme(long roleId)
    {
        if(isColorme(roleId))
            throw new IllegalArgumentException("Provided Role ID was already a colorme role!");
        guildJSON.put(COLORME_ROLE_IDS, getColormeArray().put(roleId));
        return this;
    }
    
    public boolean removeColorme(long roleId)
    {
        if(!isColorme(roleId))
            return false;
        JSONArray array = getColormeArray();
        for(int i=0; i<array.length(); i++) {
            if(array.getLong(i)==roleId)
                array.remove(i);
        }
        guildJSON.put(COLORME_ROLE_IDS, array);
        manager.put(this);
        return true;
    }
    
    public void putClearColormes()
    {
        guildJSON.put(COLORME_ROLE_IDS, new JSONArray());
    }
    
    public void clearColormes()
    {
        guildJSON.put(COLORME_ROLE_IDS, new JSONArray());
        manager.put(this);
    }
    
    
    // RESET
    public void fullReset()
    {
        guildJSON.put(PREFIXES, new JSONArray().put("|"));
        guildJSON.put(MOD_ROLE_ID, 0L);
        guildJSON.put(MOD_LOG_ID, 0L);
        guildJSON.put(MUTED_ROLE_ID, 0L);
        guildJSON.put(MUTED_USERS, new JSONArray());
        guildJSON.put(CASE_NUMBER, 0);
        guildJSON.put(CASES, new JSONObject());
        guildJSON.put(ROLEME_ROLE_IDS, new JSONArray());
        guildJSON.put(COLORME_ROLE_IDS, new JSONArray());
        manager.put(this);
    }
}
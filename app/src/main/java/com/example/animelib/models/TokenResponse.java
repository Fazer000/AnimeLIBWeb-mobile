package com.example.animelib.models;

import com.google.gson.annotations.SerializedName;

/**
 * Модель для парсинга ответа с токенами из localStorage
 */
public class TokenResponse {
    @SerializedName("token")
    private TokenData token;
    
    @SerializedName("auth")
    private AuthData auth;
    
    @SerializedName("prevUrl")
    private String prevUrl;
    
    @SerializedName("timestamp")
    private long timestamp;
    
    public TokenData getToken() { return token; }
    public void setToken(TokenData token) { this.token = token; }
    
    public AuthData getAuth() { return auth; }
    public void setAuth(AuthData auth) { this.auth = auth; }
    
    public String getPrevUrl() { return prevUrl; }
    public void setPrevUrl(String prevUrl) { this.prevUrl = prevUrl; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    /**
     * Данные токена
     */
    public static class TokenData {
        @SerializedName("token_type")
        private String tokenType;
        
        @SerializedName("expires_in")
        private long expiresIn;
        
        @SerializedName("access_token")
        private String accessToken;
        
        @SerializedName("refresh_token")
        private String refreshToken;
        
        @SerializedName("timestamp")
        private long timestamp;
        
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        
        public long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
        
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * Данные авторизации
     */
    public static class AuthData {
        @SerializedName("id")
        private int id;
        
        @SerializedName("username")
        private String username;
        
        @SerializedName("avatar")
        private AvatarData avatar;
        
        @SerializedName("balance")
        private int balance;
        
        @SerializedName("last_online_at")
        private String lastOnlineAt;
        
        @SerializedName("teams")
        private Object[] teams;
        
        @SerializedName("rolesInTeams")
        private Object[] rolesInTeams;
        
        @SerializedName("permissions")
        private Object[] permissions;
        
        @SerializedName("roles")
        private Object[] roles;
        
        @SerializedName("metadata")
        private MetadataData metadata;
        
        @SerializedName("premium")
        private PremiumData premium;
        
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public AvatarData getAvatar() { return avatar; }
        public void setAvatar(AvatarData avatar) { this.avatar = avatar; }
        
        public int getBalance() { return balance; }
        public void setBalance(int balance) { this.balance = balance; }
        
        public String getLastOnlineAt() { return lastOnlineAt; }
        public void setLastOnlineAt(String lastOnlineAt) { this.lastOnlineAt = lastOnlineAt; }
        
        public Object[] getTeams() { return teams; }
        public void setTeams(Object[] teams) { this.teams = teams; }
        
        public Object[] getRolesInTeams() { return rolesInTeams; }
        public void setRolesInTeams(Object[] rolesInTeams) { this.rolesInTeams = rolesInTeams; }
        
        public Object[] getPermissions() { return permissions; }
        public void setPermissions(Object[] permissions) { this.permissions = permissions; }
        
        public Object[] getRoles() { return roles; }
        public void setRoles(Object[] roles) { this.roles = roles; }
        
        public MetadataData getMetadata() { return metadata; }
        public void setMetadata(MetadataData metadata) { this.metadata = metadata; }
        
        public PremiumData getPremium() { return premium; }
        public void setPremium(PremiumData premium) { this.premium = premium; }
    }
    
    public static class AvatarData {
        @SerializedName("filename")
        private String filename;
        
        @SerializedName("url")
        private String url;
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
    
    public static class MetadataData {
        @SerializedName("auth_domains")
        private java.util.Map<String, String> authDomains;
        
        public java.util.Map<String, String> getAuthDomains() { return authDomains; }
        public void setAuthDomains(java.util.Map<String, String> authDomains) { this.authDomains = authDomains; }
    }
    
    public static class PremiumData {
        @SerializedName("enabled")
        private boolean enabled;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}

/*
 * Copyright 2022-2026 sephy.top
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.sephy.infra.security;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.fasterxml.jackson.annotation.JsonProperty;

import top.sephy.infra.auth.AuthenticationInfo;

public class CustomSecurityUser extends User {

    @Serial
    private static final long serialVersionUID = 1007955580804484318L;

    private final Map<String, Object> attributes = new HashMap<>();

    // @ConstructorProperties({"username", "password", "enabled",
    // "accountNonExpired", "credentialsNonExpired",
    // "accountNonLocked", "authorities"})
    public CustomSecurityUser(@JsonProperty("username") String username, @JsonProperty("password") String password,
        @JsonProperty("enabled") boolean enabled, @JsonProperty("accountNonExpired") boolean accountNonExpired,
        @JsonProperty("credentialsNonExpired") boolean credentialsNonExpired,
        @JsonProperty("accountNonLocked") boolean accountNonLocked,
        @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities,
        @JsonProperty("attributes") Map<String, Object> attributes) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        if (attributes != null) {
            this.attributes.put(AuthenticationInfo.KEY_USERNAME, username);
            this.attributes.putAll(attributes);
        }
    }

    public Long getUserId() {
        return (Long)attributes.get(AuthenticationInfo.KEY_USER_ID);
    }

    public String getNickname() {
        return (String)attributes.get(AuthenticationInfo.KEY_NICKNAME);
    }

    public Map<String, Object> getAttributes() {
        return attributes.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(attributes);
    }
}

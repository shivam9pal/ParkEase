package com.parkease.auth.security.oauth2;

import java.util.Map;

/**
 * Maps GitHub's OAuth2 attribute keys to our common interface.
 * GitHub returns: id, name, email, avatar_url, login
 */
public class GithubOAuth2UserInfo extends OAuth2UserInfo {

    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getName() {
        // GitHub users may not have a display name — fall back to login handle
        Object name = attributes.get("name");
        if (name == null || name.toString().isBlank()) {
            return (String) attributes.get("login");
        }
        return (String) name;
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("avatar_url");
    }
}

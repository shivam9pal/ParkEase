package com.parkease.auth.security.oauth2;

import java.util.Map;

/**
 * Factory — returns the correct OAuth2UserInfo implementation
 * based on the provider name (google / github).
 */
public class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {}

    public static OAuth2UserInfo getOAuth2UserInfo(
            String registrationId, Map<String, Object> attributes) {

        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "github" -> new GithubOAuth2UserInfo(attributes);
            default -> throw new RuntimeException(
                    "OAuth2 provider not supported: " + registrationId);
        };
    }
}

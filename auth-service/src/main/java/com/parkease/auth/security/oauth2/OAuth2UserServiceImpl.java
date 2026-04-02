package com.parkease.auth.security.oauth2;

import com.parkease.auth.entity.User;
import com.parkease.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Called by Spring Security after a successful OAuth2 authorization.
 * Fetches the user's profile from the provider (Google / GitHub),
 * then either creates a new User record or updates the existing one.
 */
@Service
@RequiredArgsConstructor
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. Delegate to default impl — calls the provider's /userinfo endpoint
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. Normalize attributes via factory
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory
                .getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        // 3. Email is required for all OAuth2 providers
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(
                    "Email not returned by OAuth2 provider: " + registrationId +
                            ". Please ensure your account has a public email address.");
        }

        // 4. Find existing user or register new one
        User user = userRepository.findByEmail(userInfo.getEmail())
                .map(existing -> updateExistingUser(existing, userInfo))
                .orElseGet(() -> registerNewOAuth2User(userInfo));

        // 5. Return a Spring Security principal wrapping the OAuth2 attributes + our user
        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }

    // ── Create new user from OAuth2 profile ──────────────────────────────────
    private User registerNewOAuth2User(OAuth2UserInfo userInfo) {
        User user = User.builder()
                .fullName(userInfo.getName())
                .email(userInfo.getEmail())
                .passwordHash(null)           // No password — OAuth2 only
                .role(User.Role.DRIVER)       // Default role for social login
                .isActive(true)
                .profilePicUrl(userInfo.getImageUrl())
                .build();
        return userRepository.save(user);
    }

    // ── Update profile pic / name if user already exists ─────────────────────
    private User updateExistingUser(User existing, OAuth2UserInfo userInfo) {
        existing.setFullName(userInfo.getName());
        existing.setProfilePicUrl(userInfo.getImageUrl());
        return userRepository.save(existing);
    }
}

package com.SouthMillion.user_service.service;

import com.SouthMillion.user_service.enity.User;
import com.SouthMillion.user_service.repository.UserRepo;
import org.SouthMillion.dto.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private AuthService authService;

    // =========================================================
    // verifyPassword
    // =========================================================
    @Nested
    @DisplayName("verifyPassword()")
    class VerifyPassword {

        @Test
        @DisplayName("TC-AUTH-001 [P] Xac thuc dung bang account")
        void verifyPassword_byAccount_success() {
            User user = User.builder()
                    .userId("u1")
                    .account("acc01")
                    .passHash("HASH")
                    .status(UserStatus.ACTIVE)
                    .build();
            given(userRepo.findByAccount("acc01")).willReturn(Optional.of(user));
            given(encoder.matches("rawPass", "HASH")).willReturn(true);

            User result = authService.verifyPassword("acc01", "rawPass");

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo("u1");
        }

        @Test
        @DisplayName("TC-AUTH-002 [P] Xac thuc dung bang username")
        void verifyPassword_byUsername_success() {
            User user = User.builder()
                    .userId("u1")
                    .username("Player1")
                    .passHash("HASH")
                    .status(UserStatus.ACTIVE)
                    .build();
            given(userRepo.findByAccount("Player1")).willReturn(Optional.empty());
            given(userRepo.findByUsername("Player1")).willReturn(Optional.of(user));
            given(encoder.matches("rawPass", "HASH")).willReturn(true);

            User result = authService.verifyPassword("Player1", "rawPass");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("TC-AUTH-003 [N] Sai mat khau tra ve null")
        void verifyPassword_wrongPassword_returnsNull() {
            User user = User.builder()
                    .userId("u1")
                    .account("acc01")
                    .passHash("HASH")
                    .status(UserStatus.ACTIVE)
                    .build();
            given(userRepo.findByAccount("acc01")).willReturn(Optional.of(user));
            given(encoder.matches("wrong", "HASH")).willReturn(false);

            User result = authService.verifyPassword("acc01", "wrong");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("TC-AUTH-004 [N] User khong ton tai tra ve null")
        void verifyPassword_userNotFound_returnsNull() {
            given(userRepo.findByAccount("ghost")).willReturn(Optional.empty());
            given(userRepo.findByUsername("ghost")).willReturn(Optional.empty());

            User result = authService.verifyPassword("ghost", "anyPass");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("TC-AUTH-005 [N] User bi BANNED tra ve null")
        void verifyPassword_bannedUser_returnsNull() {
            User banned = User.builder()
                    .userId("u1")
                    .account("acc01")
                    .passHash("HASH")
                    .status(UserStatus.BANNED)
                    .build();
            given(userRepo.findByAccount("acc01")).willReturn(Optional.of(banned));

            User result = authService.verifyPassword("acc01", "rawPass");

            assertThat(result).isNull();
        }
    }

    // =========================================================
    // isActive
    // =========================================================
    @Nested
    @DisplayName("isActive()")
    class IsActive {

        @Test
        @DisplayName("TC-AUTH-010 [P] User ACTIVE tra ve true")
        void isActive_activeUser_returnsTrue() {
            User user = User.builder()
                    .userId("u1")
                    .status(UserStatus.ACTIVE)
                    .build();
            given(userRepo.findById("u1")).willReturn(Optional.of(user));

            boolean result = authService.isActive("u1");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("TC-AUTH-011 [P] User BANNED tra ve false")
        void isActive_bannedUser_returnsFalse() {
            User user = User.builder()
                    .userId("u1")
                    .status(UserStatus.BANNED)
                    .build();
            given(userRepo.findById("u1")).willReturn(Optional.of(user));

            boolean result = authService.isActive("u1");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("TC-AUTH-013 [N] UserId khong ton tai tra ve false")
        void isActive_userNotFound_returnsFalse() {
            given(userRepo.findById("ghost")).willReturn(Optional.empty());

            boolean result = authService.isActive("ghost");

            assertThat(result).isFalse();
        }
    }
}

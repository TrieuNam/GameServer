package com.SouthMillion.user_service.service;

import com.SouthMillion.user_service.enity.User;
import com.SouthMillion.user_service.repository.UserRepo;
import org.SouthMillion.dto.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepo repo;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    // =========================================================
    // createUser
    // =========================================================
    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        @Test
        @DisplayName("TC-USR-001 [P] Tao user moi thanh cong")
        void createUser_success() {
            given(repo.findByAccount("user01")).willReturn(Optional.empty());
            given(repo.findByUsername("Player1")).willReturn(Optional.empty());
            given(encoder.encode("Pass123!")).willReturn("$2a$hash");

            User saved = User.builder()
                    .userId("uuid-1")
                    .account("user01")
                    .username("Player1")
                    .passHash("$2a$hash")
                    .status(UserStatus.ACTIVE)
                    .build();
            given(repo.save(any(User.class))).willReturn(saved);

            User result = userService.createUser("user01", "Player1", "Pass123!");

            assertThat(result.getUserId()).isNotNull();
            assertThat(result.getPassHash()).isEqualTo("$2a$hash");
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
            then(repo).should().save(any(User.class));
        }

        @Test
        @DisplayName("TC-USR-002 [N] Account da ton tai")
        void createUser_accountAlreadyExists_throws() {
            User existing = User.builder().account("user01").build();
            given(repo.findByAccount("user01")).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> userService.createUser("user01", "Player1", "Pass123!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account already exists");

            then(repo).should(never()).save(any());
        }

        @Test
        @DisplayName("TC-USR-003 [N] Username da ton tai")
        void createUser_usernameAlreadyExists_throws() {
            given(repo.findByAccount("user01")).willReturn(Optional.empty());
            User existing = User.builder().username("Player1").build();
            given(repo.findByUsername("Player1")).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> userService.createUser("user01", "Player1", "Pass123!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Username already exists");

            then(repo).should(never()).save(any());
        }

        @Test
        @DisplayName("TC-USR-004 [P] Password duoc ma hoa (khong luu raw)")
        void createUser_passwordIsHashed() {
            given(repo.findByAccount(anyString())).willReturn(Optional.empty());
            given(repo.findByUsername(anyString())).willReturn(Optional.empty());
            given(encoder.encode("rawPass")).willReturn("HASHED");
            given(repo.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            User result = userService.createUser("acc", "name", "rawPass");

            assertThat(result.getPassHash()).isEqualTo("HASHED");
            assertThat(result.getPassHash()).isNotEqualTo("rawPass");
        }
    }

    // =========================================================
    // login
    // =========================================================
    @Nested
    @DisplayName("login()")
    class Login {

        private User activeUser;

        @BeforeEach
        void setUp() {
            activeUser = User.builder()
                    .userId("u1")
                    .account("user01")
                    .username("Player1")
                    .passHash("$2a$hash")
                    .status(UserStatus.ACTIVE)
                    .build();
        }

        @Test
        @DisplayName("TC-USR-010 [P] Dang nhap bang account thanh cong")
        void login_byAccount_success() {
            given(repo.findByAccount("user01")).willReturn(Optional.of(activeUser));
            given(encoder.matches("Pass123!", "$2a$hash")).willReturn(true);

            User result = userService.login("user01", "Pass123!");

            assertThat(result.getUserId()).isEqualTo("u1");
        }

        @Test
        @DisplayName("TC-USR-011 [P] Dang nhap bang username thanh cong")
        void login_byUsername_success() {
            given(repo.findByAccount("Player1")).willReturn(Optional.empty());
            given(repo.findByUsername("Player1")).willReturn(Optional.of(activeUser));
            given(encoder.matches("Pass123!", "$2a$hash")).willReturn(true);

            User result = userService.login("Player1", "Pass123!");

            assertThat(result.getUserId()).isEqualTo("u1");
        }

        @Test
        @DisplayName("TC-USR-012 [N] Sai mat khau")
        void login_wrongPassword_throws() {
            given(repo.findByAccount("user01")).willReturn(Optional.of(activeUser));
            given(encoder.matches("WrongPass", "$2a$hash")).willReturn(false);

            assertThatThrownBy(() -> userService.login("user01", "WrongPass"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid password");
        }

        @Test
        @DisplayName("TC-USR-013 [N] Tai khoan khong ton tai")
        void login_userNotFound_throws() {
            given(repo.findByAccount("ghost")).willReturn(Optional.empty());
            given(repo.findByUsername("ghost")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login("ghost", "Pass123!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("TC-USR-014 [N] Tai khoan bi khoa (BANNED)")
        void login_bannedAccount_throws() {
            User banned = User.builder()
                    .userId("u2")
                    .account("user01")
                    .passHash("$2a$hash")
                    .status(UserStatus.BANNED)
                    .build();
            given(repo.findByAccount("user01")).willReturn(Optional.of(banned));

            assertThatThrownBy(() -> userService.login("user01", "Pass123!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not active");
        }
    }

    // =========================================================
    // changePassword
    // =========================================================
    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("TC-USR-020 [P] Doi mat khau thanh cong")
        void changePassword_success() {
            User user = User.builder()
                    .userId("u1")
                    .passHash("OLD_HASH")
                    .status(UserStatus.ACTIVE)
                    .build();
            given(repo.findById("u1")).willReturn(Optional.of(user));
            given(encoder.matches("oldPass", "OLD_HASH")).willReturn(true);
            given(encoder.encode("newPass")).willReturn("NEW_HASH");
            given(repo.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            userService.changePassword("u1", "oldPass", "newPass");

            assertThat(user.getPassHash()).isEqualTo("NEW_HASH");
            then(repo).should().save(user);
        }

        @Test
        @DisplayName("TC-USR-021 [N] Mat khau cu sai")
        void changePassword_wrongOldPassword_throws() {
            User user = User.builder().userId("u1").passHash("OLD_HASH").build();
            given(repo.findById("u1")).willReturn(Optional.of(user));
            given(encoder.matches("WrongOld", "OLD_HASH")).willReturn(false);

            assertThatThrownBy(() -> userService.changePassword("u1", "WrongOld", "newPass"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid old password");
        }

        @Test
        @DisplayName("TC-USR-022 [N] UserId khong ton tai")
        void changePassword_userNotFound_throws() {
            given(repo.findById("ghost")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changePassword("ghost", "old", "new"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // =========================================================
    // updateStatus
    // =========================================================
    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("TC-USR-030 [P] Khoa tai khoan thanh cong")
        void updateStatus_ban_success() {
            User user = User.builder().userId("u1").status(UserStatus.ACTIVE).build();
            given(repo.findById("u1")).willReturn(Optional.of(user));
            given(repo.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            userService.updateStatus("u1", UserStatus.BANNED);

            assertThat(user.getStatus()).isEqualTo(UserStatus.BANNED);
        }

        @Test
        @DisplayName("TC-USR-031 [P] Kich hoat lai tai khoan")
        void updateStatus_activate_success() {
            User user = User.builder().userId("u1").status(UserStatus.BANNED).build();
            given(repo.findById("u1")).willReturn(Optional.of(user));
            given(repo.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            userService.updateStatus("u1", UserStatus.ACTIVE);

            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("TC-USR-032 [N] UserId khong ton tai")
        void updateStatus_userNotFound_throws() {
            given(repo.findById("ghost")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateStatus("ghost", UserStatus.BANNED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }
    }
}

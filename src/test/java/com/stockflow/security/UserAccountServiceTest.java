package com.stockflow.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAccountService userAccountService;

    @Test
    void createNormalizesIdentityAndEncryptsPassword() {
        UserAccountForm form = validForm();
        form.setUsername("  Store.User  ");
        form.setEmail("  STORE@EXAMPLE.COM  ");
        when(passwordEncoder.encode("StrongPass1")).thenReturn("encrypted-password");
        when(userAccountRepository.save(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount saved = userAccountService.create(form);

        assertThat(saved.getUsername()).isEqualTo("store.user");
        assertThat(saved.getEmail()).isEqualTo("store@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("encrypted-password");
        verify(userAccountRepository).save(saved);
    }

    @Test
    void currentUserCannotDeactivateOwnAccount() {
        UserAccount account = account("itadmin", UserRole.IT_SYSTEMS_OFFICER, true);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> userAccountService.setActive(1L, false, "itadmin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currently signed in");
    }

    @Test
    void lastActiveItOfficerCannotBeDeactivated() {
        UserAccount account = account("system.admin", UserRole.IT_SYSTEMS_OFFICER, true);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(userAccountRepository.countByRoleAndActiveTrue(UserRole.IT_SYSTEMS_OFFICER))
                .thenReturn(1L);

        assertThatThrownBy(() -> userAccountService.setActive(1L, false, "another.admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one active IT");
    }

    @Test
    void resetPasswordRejectsShortValues() {
        assertThatThrownBy(() -> userAccountService.resetPassword(1L, "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 8 and 72");
    }

    private UserAccountForm validForm() {
        UserAccountForm form = new UserAccountForm();
        form.setFullName("Store User");
        form.setUsername("store.user");
        form.setEmail("store@example.com");
        form.setPassword("StrongPass1");
        form.setRole(UserRole.STORE_SUPERVISOR);
        form.setActive(true);
        return form;
    }

    private UserAccount account(String username, UserRole role, boolean active) {
        return new UserAccount(
                username,
                "encrypted-password",
                "Test User",
                username + "@example.com",
                role,
                active);
    }
}

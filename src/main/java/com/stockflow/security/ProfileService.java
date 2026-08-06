package com.stockflow.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class ProfileService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserAccountRepository userAccountRepository,
                          PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserAccount findByUsername(String username) {
        return userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User profile was not found"));
    }

    @Transactional(readOnly = true)
    public ProfileForm createForm(String username) {
        UserAccount account = findByUsername(username);
        ProfileForm form = new ProfileForm();
        form.setFullName(account.getFullName());
        form.setEmail(account.getEmail());
        return form;
    }

    public UserAccount updateProfile(String username, ProfileForm form) {
        UserAccount account = findByUsername(username);
        String email = normalizeEmail(form.getEmail());
        if (userAccountRepository.existsByEmailIgnoreCaseAndIdNot(email, account.getId())) {
            throw new IllegalArgumentException("That email address is already in use");
        }
        account.setFullName(form.getFullName().trim());
        account.setEmail(email);
        return userAccountRepository.save(account);
    }

    public void changePassword(String username, PasswordChangeForm form) {
        UserAccount account = findByUsername(username);
        if (!passwordEncoder.matches(form.getCurrentPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("The current password is incorrect");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("The new passwords do not match");
        }
        if (form.getNewPassword().length() < 8 || form.getNewPassword().length() > 72) {
            throw new IllegalArgumentException("New password must contain between 8 and 72 characters");
        }
        if (passwordEncoder.matches(form.getNewPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("The new password must be different from the current password");
        }
        account.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        userAccountRepository.save(account);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}

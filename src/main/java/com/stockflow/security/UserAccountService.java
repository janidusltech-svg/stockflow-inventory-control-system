package com.stockflow.security;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository,
                              PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findAll() {
        return userAccountRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName"));
    }

    @Transactional(readOnly = true)
    public UserAccount findById(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User account was not found"));
    }

    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return userAccountRepository.existsByUsernameIgnoreCase(normalizeUsername(username));
    }

    public UserAccount create(UserAccountForm form) {
        String username = normalizeUsername(form.getUsername());
        String email = normalizeEmail(form.getEmail());
        validatePassword(form.getPassword());
        validateUnique(username, email, null);

        UserAccount account = new UserAccount(
                username,
                passwordEncoder.encode(form.getPassword()),
                form.getFullName().trim(),
                email,
                form.getRole(),
                form.isActive());
        return userAccountRepository.save(account);
    }

    public UserAccount update(Long id, UserAccountForm form, String currentUsername) {
        UserAccount account = findById(id);
        String username = normalizeUsername(form.getUsername());
        String email = normalizeEmail(form.getEmail());
        validateUnique(username, email, id);

        boolean removingLastItOfficer = account.isActive()
                && account.getRole() == UserRole.IT_SYSTEMS_OFFICER
                && (!form.isActive() || form.getRole() != UserRole.IT_SYSTEMS_OFFICER)
                && userAccountRepository.countByRoleAndActiveTrue(UserRole.IT_SYSTEMS_OFFICER) <= 1;
        if (removingLastItOfficer) {
            throw new IllegalArgumentException("At least one active IT and Systems Officer account is required");
        }
        if (account.getUsername().equalsIgnoreCase(currentUsername) && !form.isActive()) {
            throw new IllegalArgumentException("You cannot deactivate the account currently signed in");
        }
        if (account.getUsername().equalsIgnoreCase(currentUsername)
                && account.getRole() != form.getRole()) {
            throw new IllegalArgumentException("You cannot change the role of the account currently signed in");
        }

        account.setUsername(username);
        account.setFullName(form.getFullName().trim());
        account.setEmail(email);
        account.setRole(form.getRole());
        account.setActive(form.isActive());
        return userAccountRepository.save(account);
    }

    public void setActive(Long id, boolean active, String currentUsername) {
        UserAccount account = findById(id);
        if (!active && account.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new IllegalArgumentException("You cannot deactivate the account currently signed in");
        }
        if (!active
                && account.isActive()
                && account.getRole() == UserRole.IT_SYSTEMS_OFFICER
                && userAccountRepository.countByRoleAndActiveTrue(UserRole.IT_SYSTEMS_OFFICER) <= 1) {
            throw new IllegalArgumentException("At least one active IT and Systems Officer account is required");
        }
        account.setActive(active);
        userAccountRepository.save(account);
    }

    public void resetPassword(Long id, String newPassword) {
        validatePassword(newPassword);
        UserAccount account = findById(id);
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccountRepository.save(account);
    }

    private void validateUnique(String username, String email, Long currentId) {
        boolean usernameTaken = currentId == null
                ? userAccountRepository.existsByUsernameIgnoreCase(username)
                : userAccountRepository.existsByUsernameIgnoreCaseAndIdNot(username, currentId);
        if (usernameTaken) {
            throw new IllegalArgumentException("That username is already in use");
        }

        boolean emailTaken = currentId == null
                ? userAccountRepository.existsByEmailIgnoreCase(email)
                : userAccountRepository.existsByEmailIgnoreCaseAndIdNot(email, currentId);
        if (emailTaken) {
            throw new IllegalArgumentException("That email address is already in use");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("Password must contain between 8 and 72 characters");
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}

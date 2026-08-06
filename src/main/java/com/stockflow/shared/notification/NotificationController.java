package com.stockflow.shared.notification;

import com.stockflow.security.CurrentUserService;
import com.stockflow.security.UserRole;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService,
                                  CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String notifications(Authentication authentication, Model model) {
        UserRole role = currentUserService.requireRole(authentication);
        model.addAttribute("notifications",
                notificationService.refreshAndFindAll(authentication.getName(), role));
        return "notifications/list";
    }

    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> unreadCount(Authentication authentication) {
        UserRole role = currentUserService.requireRole(authentication);
        long count = notificationService.refreshAndCountUnread(authentication.getName(), role);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    public String markRead(@PathVariable Long id,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            notificationService.markRead(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        notificationService.markAllRead(authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read");
        return "redirect:/notifications";
    }
}

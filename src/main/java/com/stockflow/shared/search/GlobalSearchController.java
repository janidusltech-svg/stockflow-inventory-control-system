package com.stockflow.shared.search;

import com.stockflow.security.CurrentUserService;
import com.stockflow.security.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/search")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;
    private final CurrentUserService currentUserService;

    public GlobalSearchController(GlobalSearchService globalSearchService,
                                  CurrentUserService currentUserService) {
        this.globalSearchService = globalSearchService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String search(@RequestParam(required = false) String q,
                         Authentication authentication,
                         Model model) {
        String query = q == null ? "" : q.trim();
        UserRole role = currentUserService.requireRole(authentication);
        model.addAttribute("query", query);
        model.addAttribute("searched", !query.isBlank());
        model.addAttribute("queryTooShort", !query.isBlank() && query.length() < 2);
        model.addAttribute("results", globalSearchService.search(query, role));
        return "search/results";
    }
}

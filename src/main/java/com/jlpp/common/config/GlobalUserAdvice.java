package com.jlpp.common.config;

import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalUserAdvice {

    private final UserRepository userRepository;

    @ModelAttribute
    public void populateCurrentUser(Model model, Principal principal, HttpSession session) {
        User currentUser = null;
        if (principal != null && principal.getName() != null) {
            String identifier = principal.getName().trim();
            currentUser = userRepository.findByEmailIgnoreCase(identifier)
                    .or(() -> userRepository.findByUsername(identifier))
                    .orElse(null);
        } else if (session != null && session.getAttribute("CURRENT_LEARNER_ID") != null) {
            Long userId = (Long) session.getAttribute("CURRENT_LEARNER_ID");
            currentUser = userRepository.findById(userId).orElse(null);
        } else if (session != null && session.getAttribute("CURRENT_USER_EMAIL") != null) {
            String email = (String) session.getAttribute("CURRENT_USER_EMAIL");
            currentUser = userRepository.findByEmailIgnoreCase(email).orElse(null);
        }

        if (currentUser != null) {
            String roleName = currentUser.getRole() != null ? currentUser.getRole().name() : "LEARNER";
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("currentUserFullName", currentUser.getFullName());
            model.addAttribute("currentUserAvatar", currentUser.getAvatarUrl());
            model.addAttribute("userName", currentUser.getFullName());
            model.addAttribute("role", roleName);
            model.addAttribute("currentRole", roleName);
            model.addAttribute("isAuthenticated", true);
            if (session != null) {
                session.setAttribute("role", roleName);
                session.setAttribute("CURRENT_LEARNER_ID", currentUser.getId());
                session.setAttribute("CURRENT_USER_EMAIL", currentUser.getEmail());
            }
        } else {
            model.addAttribute("role", "GUEST");
            model.addAttribute("currentRole", "GUEST");
            model.addAttribute("isAuthenticated", false);
        }
    }
}

package com.dionialves.AsteraComm.profile;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dionialves.AsteraComm.user.User;
import com.dionialves.AsteraComm.user.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/modal")
    public String profileModal(Model model) {
        model.addAttribute("user", getCurrentUser());
        return "fragments/profile/modal :: modal";
    }

    @GetMapping("/password-modal")
    public String passwordModal() {
        return "fragments/profile/password-modal :: modal";
    }

    @PutMapping
    public String updateProfile(@RequestParam String name, Model model) {
        User user = getCurrentUser();
        userRepository.findById(user.getId()).ifPresent(u -> {
            u.setName(name.strip());
            userRepository.save(u);
        });
        model.addAttribute("toastMsg", "Perfil atualizado com sucesso.");
        model.addAttribute("toastType", "success");
        return "fragments/profile/response :: ok";
    }

    @PutMapping("/password")
    public String updatePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {

        User user = getCurrentUser();
        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!passwordEncoder.matches(currentPassword, dbUser.getPassword())) {
            model.addAttribute("error", "Senha atual incorreta.");
            return "fragments/profile/password-modal :: modal";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "A confirmação da senha não corresponde.");
            return "fragments/profile/password-modal :: modal";
        }

        if (newPassword.length() < 8) {
            model.addAttribute("error", "A nova senha deve ter pelo menos 8 caracteres.");
            return "fragments/profile/password-modal :: modal";
        }

        dbUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(dbUser);

        model.addAttribute("toastMsg", "Senha alterada com sucesso.");
        model.addAttribute("toastType", "success");
        return "fragments/profile/response :: ok";
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

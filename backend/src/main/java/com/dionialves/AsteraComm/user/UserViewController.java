package com.dionialves.AsteraComm.user;

import com.dionialves.AsteraComm.user.dto.UserCreateDTO;
import com.dionialves.AsteraComm.user.dto.UserResponseDTO;
import com.dionialves.AsteraComm.user.dto.UserUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import java.security.SecureRandom;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserViewController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String list() {
        return "pages/users/list";
    }

    @GetMapping("/table")
    public String table(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Boolean enabled,
            Model model) {

        model.addAttribute("users", fetchPage(page, size, sort, search, enabled));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("enabledFilter", enabled);
        return "pages/users/table :: table";
    }

    @GetMapping("/modal/new")
    public String modalNew(Model model) {
        model.addAttribute("user", null);
        return "pages/users/modal :: modal";
    }

    @GetMapping("/{id}/modal")
    public String modalEdit(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        return "pages/users/modal :: modal";
    }

    @PostMapping
    public String create(
            @RequestParam String name,
            @RequestParam String username,
            @RequestParam String password,
            Model model) {
        try {
            UserResponseDTO user = userService.create(new UserCreateDTO(name, username, password));
            model.addAttribute("user", user);
            model.addAttribute("toastMsg", "Usuário criado com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
            model.addAttribute("users", fetchPage(0, 20, "name,asc", "", null));
            model.addAttribute("sort", "name,asc");
            model.addAttribute("enabledFilter", null);
        } catch (Exception e) {
            model.addAttribute("user", null);
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        return "pages/users/modal :: modal";
    }

    @PutMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) Boolean enabled,
            Model model) {
        try {
            userService.update(id, new UserUpdateDTO(name, Boolean.TRUE.equals(enabled)));
            model.addAttribute("toastMsg", "Usuário atualizado com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
        } catch (Exception e) {
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        model.addAttribute("user", userService.findById(id));
        return "pages/users/modal :: modal";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, Model model) {
        try {
            userService.delete(id);
            model.addAttribute("toastMsg", "Usuário removido com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
            model.addAttribute("tableUrl", "/users/table");
            return "fragments/modal-close :: close";
        } catch (Exception e) {
            model.addAttribute("user", userService.findById(id));
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
            return "pages/users/modal :: modal";
        }
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, Model model) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            String tempPassword = generateTempPassword();
            user.setPassword(passwordEncoder.encode(tempPassword));
            userRepository.save(user);
            model.addAttribute("toastMsg", "Senha resetada. Nova senha temporária: " + tempPassword);
            model.addAttribute("toastType", "success");
        } catch (Exception e) {
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        model.addAttribute("user", userService.findById(id));
        return "pages/users/modal :: modal";
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String tableFullResponse(Model model) {
        model.addAttribute("users", fetchPage(0, 20, "name,asc", "", null));
        model.addAttribute("search", "");
        model.addAttribute("sort", "name,asc");
        model.addAttribute("enabledFilter", null);
        return "pages/users/table";
    }

    private static String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private Page<UserResponseDTO> fetchPage(int page, int size, String sort, String search, Boolean enabled) {
        return userService.findAll(search.isBlank() ? null : search, enabled, page, size, sort);
    }
}

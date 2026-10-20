package com.gohar.platform.seckit.presentation.controller;

import com.gohar.platform.seckit.application.command.RegisterUserCommand;
import com.gohar.platform.seckit.application.result.RegisterUserResult;
import com.gohar.platform.seckit.application.usecases.RegisterUserUseCase;
import com.gohar.platform.seckit.domain.exception.DomainValidationException;
import com.gohar.platform.seckit.domain.exception.DuplicateEmailException;
import com.gohar.platform.seckit.presentation.form.RegistrationForm;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller handling user registration form display, submission, validation, and PRG navigation.
 */
@Controller
public class RegistrationController {

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    private final RegisterUserUseCase registerUserUseCase;

    public RegistrationController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("registrationForm") RegistrationForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            form.clearPassword();
            return "register";
        }

        try {
            RegisterUserCommand command = new RegisterUserCommand(
                form.getName(),
                form.getEmail(),
                form.getPassword()
            );

            RegisterUserResult result = registerUserUseCase.execute(command);
            log.info("Successfully registered user account with ID: {}", result.userId());

            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Registration completed successfully! Please sign in with your new account."
            );
            return "redirect:/login";

        } catch (DuplicateEmailException ex) {
            log.warn("Registration rejected for existing email: {}", form.getEmail());
            form.clearPassword();
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            return "register";

        } catch (DomainValidationException ex) {
            log.warn("Registration domain validation failure: {}", ex.getMessage());
            form.clearPassword();
            bindingResult.reject("validation", ex.getMessage());
            return "register";
        }
    }
}

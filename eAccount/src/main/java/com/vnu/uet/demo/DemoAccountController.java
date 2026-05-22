package com.vnu.uet.demo;

import com.vnu.uet.service.dto.EmailRequestDTO;
import com.vnu.uet.service.dto.ProfileDTO;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoAccountController {

    private static final String DEFAULT_ACCOUNT_USER = "demo.admin@uetflow.local";
    private final DemoAccountStore store;

    public DemoAccountController(DemoAccountStore store) {
        this.store = store;
    }

    @GetMapping("/account")
    public ResponseEntity<Map<String, Object>> getAccount(
        @RequestHeader(value = "X-Demo-User", required = false) String demoUser,
        @RequestParam(value = "demoUserEmail", required = false) String demoUserEmail
    ) {
        return ResponseEntity.ok(store.getAccount(resolveUser(demoUser, demoUserEmail, DEFAULT_ACCOUNT_USER)));
    }

    @GetMapping("/account/profile")
    public ResponseEntity<Map<String, Object>> getProfile(
        @RequestHeader(value = "X-Demo-User", required = false) String demoUser,
        @RequestParam(value = "demoUserEmail", required = false) String demoUserEmail
    ) {
        return ResponseEntity.ok(store.getProfile(resolveUser(demoUser, demoUserEmail, DEFAULT_ACCOUNT_USER)));
    }

    @PostMapping("/account/profile")
    public ResponseEntity<Map<String, Object>> createProfile(@Valid @RequestBody ProfileDTO dto) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("email", dto.getEmail());
        response.put("generatedPassword", "demo-" + Math.abs(dto.getEmail().hashCode()));
        response.put("message", "Tài khoản demo đã được tạo thành công");
        response.put("data", store.createUser(dto));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/account/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
        @RequestHeader(value = "X-Demo-User", required = false) String demoUser,
        @RequestParam(value = "demoUserEmail", required = false) String demoUserEmail,
        @RequestBody ProfileDTO dto
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("message", "Cập nhật thông tin demo thành công");
        response.put("data", store.updateCurrentUser(resolveUser(demoUser, demoUserEmail, DEFAULT_ACCOUNT_USER), dto));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/management/account/create")
    public ResponseEntity<Map<String, Object>> createAccount(@Valid @RequestBody ProfileDTO dto) {
        store.createUser(dto);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("message", "Tài khoản demo đã được tạo thành công");
        response.put("email", dto.getEmail());
        response.put("tempPassword", "demo-" + Math.abs(dto.getEmail().hashCode()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/management/account/search")
    public ResponseEntity<Map<String, Object>> searchAccount(@RequestBody EmailRequestDTO dto) {
        return ResponseEntity.ok(store.searchUser(dto.getEmail()));
    }

    @GetMapping("/management/account/users")
    public ResponseEntity<Map<String, Object>> listUsers() {
        return ResponseEntity.ok(Map.of("items", store.listUsers(), "total", store.listUsers().size()));
    }

    @GetMapping("/demo/users")
    public ResponseEntity<Map<String, Object>> listDemoUsers() {
        return ResponseEntity.ok(Map.of("items", store.listUsers(), "total", store.listUsers().size()));
    }

    @GetMapping("/demo/users/{email}")
    public ResponseEntity<Map<String, Object>> getDemoUser(@PathVariable("email") String email) {
        return ResponseEntity.ok(store.getUserDetail(email));
    }

    @ExceptionHandler(DemoAccountStore.DemoNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(DemoAccountStore.DemoNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage(), "status", "error"));
    }

    private String resolveUser(String headerUser, String queryUser, String fallback) {
        if (headerUser != null && !headerUser.isBlank()) {
            return headerUser;
        }
        if (queryUser != null && !queryUser.isBlank()) {
            return queryUser;
        }
        return fallback;
    }
}

package com.vnu.uet.demo;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner")
public class DemoFormController {

    @GetMapping("/menu-views")
    public ResponseEntity<?> listForms() {
        return ResponseEntity.ok(DemoFormStore.list());
    }

    @GetMapping("/form")
    public ResponseEntity<?> getForm(@RequestParam String formId) {
        Map<String, Object> form = DemoFormStore.get(formId);
        if (form == null) {
            return ResponseEntity.ok(Map.of("message", "FormId not exist", "success", false));
        }
        return ResponseEntity.ok(form);
    }

    @PostMapping("/form")
    public ResponseEntity<?> createForm(@RequestBody(required = false) Map<String, Object> payload) {
        String formId = payload != null && payload.get("formId") != null ? String.valueOf(payload.get("formId")) : "FORM-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String formName = payload != null && payload.get("formName") != null ? String.valueOf(payload.get("formName")) : "Demo Form";
        return ResponseEntity.ok(DemoFormStore.create(formId, formName));
    }

    @GetMapping("/form/change-status")
    public ResponseEntity<?> publishForm(@RequestParam String formId) {
        Map<String, Object> form = DemoFormStore.publish(formId);
        if (form == null) {
            return ResponseEntity.ok("formId is not existed");
        }
        return ResponseEntity.ok(form);
    }

    @PostMapping("/form-data")
    public ResponseEntity<?> saveFormData(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("dataId", "DATA-" + UUID.randomUUID(), "status", "saved", "formData", request));
    }
}

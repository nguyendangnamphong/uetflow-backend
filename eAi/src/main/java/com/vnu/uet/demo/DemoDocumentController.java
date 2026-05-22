package com.vnu.uet.demo;

import com.vnu.uet.service.dto.DocumentMapResponseDTO;
import com.vnu.uet.service.dto.FilledFormResultDTO;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api")
public class DemoDocumentController {

    private final DemoDocumentStore store;

    public DemoDocumentController(DemoDocumentStore store) {
        this.store = store;
    }

    @GetMapping("/document-maps/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "mode", "demo"));
    }

    @PostMapping(value = "/document-maps", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentMapResponseDTO> uploadDocument(
        @RequestPart(value = "file", required = false) MultipartFile file,
        @RequestParam(value = "formName", required = false) String formName
    ) {
        DemoDocumentStore.DemoTask task = store.createAsyncTask(file != null ? file.getOriginalFilename() : null, formName);
        return ResponseEntity.ok(task.toStatusResponse());
    }

    @GetMapping("/document-maps/{taskId}/status")
    public ResponseEntity<DocumentMapResponseDTO> getStatus(@PathVariable Long taskId) {
        return ResponseEntity.ok(store.getTask(taskId).toStatusResponse());
    }

    @GetMapping("/document-maps/{taskId}/result")
    public ResponseEntity<FilledFormResultDTO> getResult(@PathVariable Long taskId) {
        return ResponseEntity.ok(store.getTask(taskId).result());
    }

    @GetMapping("/document-maps/{taskId}/raw-text")
    public ResponseEntity<String> getRawText(@PathVariable Long taskId) {
        return ResponseEntity.ok(store.getTask(taskId).rawText());
    }

    @PostMapping(value = "/document-maps/sync", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FilledFormResultDTO> uploadSync(
        @RequestPart(value = "file", required = false) MultipartFile file,
        @RequestParam(value = "formName", required = false) String formName
    ) {
        return ResponseEntity.ok(store.syncResult(file != null ? file.getOriginalFilename() : null, formName));
    }

    @PostMapping("/document-maps/demo-extract")
    public ResponseEntity<Map<String, Object>> demoExtract(@RequestBody(required = false) DemoDocumentRequest request) {
        String fileName = request != null ? request.getFileName() : null;
        String formName = request != null ? request.getFormName() : null;
        return ResponseEntity.ok(store.toExtractResponse(fileName, formName));
    }

    @ExceptionHandler(DemoDocumentStore.DemoTaskNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(DemoDocumentStore.DemoTaskNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "error", "message", exception.getMessage()));
    }
}

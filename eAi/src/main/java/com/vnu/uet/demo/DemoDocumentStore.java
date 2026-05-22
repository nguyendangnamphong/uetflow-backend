package com.vnu.uet.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnu.uet.service.dto.DocumentMapResponseDTO;
import com.vnu.uet.service.dto.FilledFormResultDTO;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class DemoDocumentStore {

    private static final String SPECIAL_FILE = "don-nghi-phep-le-van-minh.pdf";

    private final AtomicLong sequence = new AtomicLong(5000);
    private final ConcurrentHashMap<Long, DemoTask> tasks = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> extract(String fileName, String formName) {
        String normalizedFileName = normalize(fileName);
        boolean specialCase = normalizedFileName.contains(SPECIAL_FILE);
        Map<String, Object> extractedData = baseExtraction(specialCase ? "Lê Văn Minh" : "Nguyễn Văn Demo");
        extractedData.put("sourceFileName", normalizedFileName.isBlank() ? SPECIAL_FILE : normalizedFileName);
        extractedData.put("formName", defaultText(formName, "Đơn nghỉ phép"));
        extractedData.put("matchedPreset", specialCase ? SPECIAL_FILE : "generic-demo");
        extractedData.put("status", "COMPLETED");
        extractedData.put("rawText", specialCase ? rawTextForMinh() : rawTextGeneric());
        extractedData.put("filledData", new LinkedHashMap<>(extractedData));
        extractedData.put("missingFields", List.of());
        return extractedData;
    }

    public DemoTask createAsyncTask(String fileName, String formName) {
        Map<String, Object> extracted = extract(fileName, formName);
        long taskId = sequence.incrementAndGet();
        FilledFormResultDTO result = toResult(taskId, extracted);
        DemoTask task = new DemoTask(taskId, "COMPLETED", "Document processing completed in demo mode", (String) extracted.get("rawText"), result);
        tasks.put(taskId, task);
        return task;
    }

    public DemoTask getTask(Long taskId) {
        DemoTask task = tasks.get(taskId);
        if (task == null) {
            throw new DemoTaskNotFoundException("Demo extraction not found: " + taskId);
        }
        return task;
    }

    public FilledFormResultDTO syncResult(String fileName, String formName) {
        long taskId = sequence.incrementAndGet();
        return toResult(taskId, extract(fileName, formName));
    }

    public Map<String, Object> toExtractResponse(String fileName, String formName) {
        Map<String, Object> extracted = extract(fileName, formName);
        FilledFormResultDTO result = toResult(sequence.incrementAndGet(), extracted);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("documentType", "leave_request");
        response.put("fullName", extracted.get("fullName"));
        response.put("leaveType", "Nghỉ phép");
        response.put("fromDate", "2026-05-22");
        response.put("toDate", "2026-05-23");
        response.put("reason", "Việc gia đình");
        response.put("confidence", 0.96d);
        response.put("title", "Đơn xin nghỉ phép");
        response.put("department", "Phòng Nhân sự");
        response.put("phone", "0912345604");
        response.put("rawText", result.getRawText());
        response.put("filledData", readJson(result.getFilledData()));
        response.put("missingFields", List.of());
        response.put("extractionId", result.getExtractionId());
        response.put("processedAt", Instant.now().toString());
        return response;
    }

    private FilledFormResultDTO toResult(long taskId, Map<String, Object> extracted) {
        FilledFormResultDTO result = new FilledFormResultDTO();
        result.setExtractionId(taskId);
        result.setFormName(String.valueOf(extracted.get("formName")));
        result.setConfidence(0.96d);
        result.setMissingFields("[]");
        result.setRawText(String.valueOf(extracted.get("rawText")));
        result.setFilledData(writeJson(extracted));
        return result;
    }

    private Map<String, Object> baseExtraction(String fullName) {
        Map<String, Object> extractedData = new LinkedHashMap<>();
        extractedData.put("documentType", "leave_request");
        extractedData.put("title", "Đơn xin nghỉ phép");
        extractedData.put("fullName", fullName);
        extractedData.put("leaveType", "Nghỉ phép");
        extractedData.put("fromDate", "2026-05-22");
        extractedData.put("toDate", "2026-05-23");
        extractedData.put("reason", "Việc gia đình");
        extractedData.put("confidence", 0.96d);
        extractedData.put("department", "Phòng Nhân sự");
        extractedData.put("phone", "0912345604");
        extractedData.put("requesterEmail", "demo.requester@uetflow.local");
        extractedData.put("approverEmail", "demo.approver@uetflow.local");
        return extractedData;
    }

    private String rawTextForMinh() {
        return "ĐƠN XIN NGHỈ PHÉP\nHọ tên: Lê Văn Minh\nLoại nghỉ: Nghỉ phép\nTừ ngày: 2026-05-22\nĐến ngày: 2026-05-23\nLý do: Việc gia đình";
    }

    private String rawTextGeneric() {
        return "Tài liệu demo đã được xử lý trong mock mode. Dữ liệu được sinh sẵn để phục vụ trình diễn.";
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize demo payload", exception);
        }
    }

    private Object readJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException exception) {
            return json;
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim().toLowerCase();
    }

    public record DemoTask(Long taskId, String status, String message, String rawText, FilledFormResultDTO result) {
        public DocumentMapResponseDTO toStatusResponse() {
            return new DocumentMapResponseDTO(String.valueOf(taskId), status, message);
        }
    }

    public static class DemoTaskNotFoundException extends RuntimeException {
        public DemoTaskNotFoundException(String message) {
            super(message);
        }
    }
}

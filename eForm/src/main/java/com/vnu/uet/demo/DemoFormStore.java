package com.vnu.uet.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DemoFormStore {

    private static final Map<String, Map<String, Object>> FORMS = new ConcurrentHashMap<>();

    static {
        put("FORM-001", "Leave Request", 2);
        put("FORM-002", "Payment Request", 2);
        put("FORM-003", "Asset Purchase", 1);
    }

    private DemoFormStore() {}

    public static List<Map<String, Object>> list() {
        return new ArrayList<>(FORMS.values());
    }

    public static Map<String, Object> get(String formId) {
        return FORMS.get(formId);
    }

    public static Map<String, Object> create(String formId, String formName) {
        Map<String, Object> form = put(formId, formName, 1);
        return form;
    }

    public static Map<String, Object> publish(String formId) {
        Map<String, Object> form = FORMS.get(formId);
        if (form != null) {
            form.put("statusForm", 2);
        }
        return form;
    }

    private static Map<String, Object> put(String formId, String formName, int status) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("formId", formId);
        form.put("formName", formName);
        form.put("jsonForm", "{\"fields\":[{\"code\":\"reason\",\"label\":\"Reason\",\"type\":\"text\"}]}");
        form.put("variableArr", List.of(Map.of("code", "reason", "variableName", "Reason", "variableType", "text")));
        form.put("statusForm", status);
        FORMS.put(formId, form);
        return form;
    }
}

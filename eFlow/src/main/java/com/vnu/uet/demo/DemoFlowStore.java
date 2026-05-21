package com.vnu.uet.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DemoFlowStore {

    private static final Map<Long, Map<String, Object>> FLOWS = new ConcurrentHashMap<>();

    static {
        put(1L, "Leave Approval Flow", "FORM-001", "Published");
        put(2L, "Payment Approval Flow", "FORM-002", "Published");
        put(3L, "Asset Approval Flow", "FORM-003", "Draft");
    }

    private DemoFlowStore() {}

    public static List<Map<String, Object>> list() {
        return new ArrayList<>(FLOWS.values());
    }

    public static Map<String, Object> get(Long id) {
        return FLOWS.get(id);
    }

    public static Map<String, Object> publish(Long id) {
        Map<String, Object> flow = FLOWS.get(id);
        if (flow != null) {
            flow.put("status", "Published");
        }
        return flow;
    }

    public static Map<String, Object> firstActionPlan(Long flowId) {
        Map<String, Object> flow = FLOWS.getOrDefault(flowId, FLOWS.get(1L));
        return Map.of(
            "nodeId",
            101L,
            "performers",
            List.of(Map.of("userId", "demo.approver@uetflow.local", "email", "demo.approver@uetflow.local", "orderExecution", 1)),
            "forms",
            List.of(Map.of("targetFormId", flow.get("formId"), "sourceFormId", flow.get("formId")))
        );
    }

    public static Map<String, Object> actionPlan(Long nodeId) {
        if (nodeId == 101L) {
            return Map.of(
                "nodeId",
                101L,
                "performers",
                List.of(Map.of("userId", "demo.approver@uetflow.local", "email", "demo.approver@uetflow.local", "orderExecution", 1)),
                "forms",
                List.of(Map.of("targetFormId", "FORM-001", "sourceFormId", "FORM-001"))
            );
        }
        return Map.of("nodeId", nodeId, "performers", List.of(), "forms", List.of());
    }

    public static Map<String, Object> nextNode(Long flowId, Long currentNodeId) {
        if (currentNodeId == 101L) {
            return Map.of("flowId", flowId, "currentNodeId", currentNodeId, "nextNodeId", 102L);
        }
        return Map.of("flowId", flowId, "currentNodeId", currentNodeId, "nextNodeId", null);
    }

    private static void put(Long id, String flowName, String formId, String status) {
        Map<String, Object> flow = new LinkedHashMap<>();
        flow.put("id", id);
        flow.put("flowId", id);
        flow.put("flowName", flowName);
        flow.put("flowGroup", "demo");
        flow.put("status", status);
        flow.put("formId", formId);
        FLOWS.put(id, flow);
    }
}

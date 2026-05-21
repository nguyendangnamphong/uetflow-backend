package com.mycompany.erequest.demo;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/request")
public class DemoRequestController {

    private static final String DEFAULT_DEMO_CREATOR = "demo.requester@uetflow.local";
    private static final String DEFAULT_DEMO_APPROVER = "demo.approver@uetflow.local";

    private final DemoFlowClient demoFlowClient;

    public DemoRequestController(DemoFlowClient demoFlowClient) {
        this.demoFlowClient = demoFlowClient;
    }

    @GetMapping("/workflows")
    public ResponseEntity<?> getWorkflows() {
        try {
            return ResponseEntity.ok(demoFlowClient.getWorkflows(Map.of()));
        } catch (Exception e) {
            return ResponseEntity.ok(List.of(Map.of("flowId", 1, "flowName", "Demo Flow 1", "status", "Demo")));
        }
    }

    @PostMapping("/ticket/init")
    public ResponseEntity<?> initTicket(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> payload) {
        Long flowId = payload != null && payload.get("flowId") != null ? Long.valueOf(String.valueOf(payload.get("flowId"))) : 1L;
        String ticketName = payload != null && payload.get("ticketName") != null ? String.valueOf(payload.get("ticketName")) : "Demo request";
        String creator = resolveUser(request, DEFAULT_DEMO_CREATOR);
        String performer = DEFAULT_DEMO_APPROVER;
        Long nodeId = 101L;
        try {
            Map<String, Object> plan = demoFlowClient.getFirstActionPlan(flowId);
            Object planNodeId = plan.get("nodeId");
            nodeId = planNodeId != null ? Long.valueOf(String.valueOf(planNodeId)) : 101L;
            Object performersObject = plan.get("performers");
            Object first = performersObject instanceof List<?> list && !list.isEmpty() ? list.get(0) : null;
            if (first instanceof Map<?, ?> map) {
                Object email = map.get("email");
                performer = email != null ? String.valueOf(email) : DEFAULT_DEMO_APPROVER;
            }
        } catch (Exception ignored) {}
        Map<String, Object> ticket = DemoRequestStore.create(flowId, ticketName, creator, performer, nodeId);
        return ResponseEntity.ok(Map.of("ticketId", ticket.get("ticketId"), "status", ticket.get("status"), "performer", performer));
    }

    @GetMapping("/tickets/my-requests")
    public ResponseEntity<?> myRequests(HttpServletRequest request) {
        String creator = resolveUser(request, DEFAULT_DEMO_CREATOR);
        List<Map<String, Object>> content = DemoRequestStore.myRequests(creator);
        return ResponseEntity.ok(Map.of("content", content, "totalElements", content.size()));
    }

    @GetMapping("/tickets/pending-tasks")
    public ResponseEntity<?> pending(HttpServletRequest request) {
        String approver = resolveUser(request, DEFAULT_DEMO_APPROVER);
        List<Map<String, Object>> content = DemoRequestStore.pending(approver);
        return ResponseEntity.ok(Map.of("content", content, "totalElements", content.size()));
    }

    @GetMapping("/ticket/{ticketId}/detail")
    public ResponseEntity<?> detail(@PathVariable Long ticketId) {
        Map<String, Object> ticket = DemoRequestStore.get(ticketId);
        return ticket == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(ticket);
    }

    @GetMapping("/ticket/{ticketId}/history")
    public ResponseEntity<?> history(@PathVariable Long ticketId) {
        return ResponseEntity.ok(DemoRequestStore.history(ticketId));
    }

    @PostMapping("/ticket/{ticketId}/submit")
    public ResponseEntity<?> submit(HttpServletRequest request, @PathVariable Long ticketId, @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> ticket = DemoRequestStore.get(ticketId);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }
        Long currentNodeId = Long.valueOf(String.valueOf(DemoRequestStore.history(ticketId).get(DemoRequestStore.history(ticketId).size() - 1).get("nodeId")));
        Long nextNodeId = null;
        String nextPerformer = null;
        try {
            Map<String, Object> next = demoFlowClient.getNextNode(Long.valueOf(String.valueOf(ticket.get("flowId"))), currentNodeId);
            if (next.get("nextNodeId") != null) {
                nextNodeId = Long.valueOf(String.valueOf(next.get("nextNodeId")));
                Map<String, Object> plan = demoFlowClient.getActionPlan(nextNodeId);
                Object performersObject = plan.get("performers");
                Object first = performersObject instanceof List<?> list && !list.isEmpty() ? list.get(0) : null;
                if (first instanceof Map<?, ?> map) {
                    Object email = map.get("email");
                    nextPerformer = email != null ? String.valueOf(email) : resolveUser(request, DEFAULT_DEMO_APPROVER);
                }
            }
        } catch (Exception ignored) {}
        DemoRequestStore.submit(ticketId, nextNodeId, nextPerformer);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "performer", nextPerformer));
    }

    @PostMapping("/ticket/{ticketId}/action")
    public ResponseEntity<?> action(@PathVariable Long ticketId, @RequestBody Map<String, Object> payload) {
        String action = String.valueOf(payload.getOrDefault("action", ""));
        Map<String, Object> ticket = DemoRequestStore.action(ticketId, action);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("action", action, "ticketStatus", ticket.get("status")));
    }

    private String resolveUser(HttpServletRequest request, String fallback) {
        String header = request.getHeader("X-Demo-User");
        if (header != null && !header.isBlank()) {
            return header;
        }
        String query = request.getParameter("demoUserEmail");
        if (query != null && !query.isBlank()) {
            return query;
        }
        return fallback;
    }
}

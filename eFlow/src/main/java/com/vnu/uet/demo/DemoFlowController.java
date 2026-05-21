package com.vnu.uet.demo;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoFlowController {

    @PostMapping("/api/workflow")
    public ResponseEntity<?> listFlows(@RequestBody(required = false) Map<String, Object> request) {
        return ResponseEntity.ok(DemoFlowStore.list());
    }

    @GetMapping("/api/workflow/{flowId}/summary")
    public ResponseEntity<?> getFlowDetail(@PathVariable Long flowId) {
        Map<String, Object> flow = DemoFlowStore.get(flowId);
        if (flow == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(flow);
    }

    @PostMapping("/api/workflow/{flowId}/status")
    public ResponseEntity<?> publishFlow(@PathVariable Long flowId, @RequestBody(required = false) String status) {
        Map<String, Object> flow = DemoFlowStore.publish(flowId);
        if (flow == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(flow);
    }

    @GetMapping("/api/workflow/node/{nodeId}/config")
    public ResponseEntity<?> getNodeConfig(@PathVariable Long nodeId) {
        return ResponseEntity.ok(Map.of("nodeId", nodeId, "nodeType", "user_task", "flowId", 1L));
    }

    @GetMapping("/api/internal/node/{nodeId}/action-plan")
    public ResponseEntity<?> getActionPlan(@PathVariable Long nodeId) {
        return ResponseEntity.ok(DemoFlowStore.actionPlan(nodeId));
    }

    @GetMapping("/api/internal/flow/{flowId}/first-action-plan")
    public ResponseEntity<?> getFirstActionPlan(@PathVariable Long flowId) {
        return ResponseEntity.ok(DemoFlowStore.firstActionPlan(flowId));
    }

    @GetMapping("/api/internal/flow/{flowId}/next-node")
    public ResponseEntity<?> getNextNode(@PathVariable Long flowId, @RequestParam Long currentNodeId) {
        return ResponseEntity.ok(DemoFlowStore.nextNode(flowId, currentNodeId));
    }

    @GetMapping("/api/proxy/eform/published-forms")
    public ResponseEntity<?> getPublishedForms() {
        return ResponseEntity.ok(
            List.of(
                Map.of("formId", "FORM-001", "formName", "Leave Request"),
                Map.of("formId", "FORM-002", "formName", "Payment Request"),
                Map.of("formId", "FORM-003", "formName", "Asset Purchase")
            )
        );
    }
}

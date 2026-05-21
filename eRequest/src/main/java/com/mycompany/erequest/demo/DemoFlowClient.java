package com.mycompany.erequest.demo;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "demoFlowClient", url = "${APPLICATION_CLIENT_EFLOW_URL:http://eflow:8083}")
public interface DemoFlowClient {
    @PostMapping("/api/workflow")
    List<Map<String, Object>> getWorkflows(@RequestBody(required = false) Map<String, Object> request);

    @GetMapping("/api/internal/flow/{flowId}/first-action-plan")
    Map<String, Object> getFirstActionPlan(@PathVariable("flowId") Long flowId);

    @GetMapping("/api/internal/node/{nodeId}/action-plan")
    Map<String, Object> getActionPlan(@PathVariable("nodeId") Long nodeId);

    @GetMapping("/api/internal/flow/{flowId}/next-node")
    Map<String, Object> getNextNode(@PathVariable("flowId") Long flowId, @RequestParam("currentNodeId") Long currentNodeId);
}

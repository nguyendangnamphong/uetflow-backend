package com.mycompany.erequest.web.rest;

import com.mycompany.erequest.client.EAccountClient;
import com.mycompany.erequest.client.EFlowClient;
import com.mycompany.erequest.client.EFormClient;
import com.mycompany.erequest.domain.Ticket;
import com.mycompany.erequest.domain.TicketStep;
import com.mycompany.erequest.repository.TicketRepository;
import com.mycompany.erequest.repository.TicketStepRepository;
import com.mycompany.erequest.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/request")
public class ERequestCustomResource {

    private static final Logger LOG = LoggerFactory.getLogger(ERequestCustomResource.class);
    private static final String DEFAULT_DEMO_CREATOR = "demo.requester@uetflow.local";
    private static final String DEFAULT_DEMO_APPROVER = "demo.approver@uetflow.local";

    private final EAccountClient eAccountClient;
    private final EFormClient eFormClient;
    private final EFlowClient eFlowClient;
    private final TicketRepository ticketRepository;
    private final TicketStepRepository ticketStepRepository;

    public ERequestCustomResource(
        EAccountClient eAccountClient,
        EFormClient eFormClient,
        EFlowClient eFlowClient,
        TicketRepository ticketRepository,
        TicketStepRepository ticketStepRepository
    ) {
        this.eAccountClient = eAccountClient;
        this.eFormClient = eFormClient;
        this.eFlowClient = eFlowClient;
        this.ticketRepository = ticketRepository;
        this.ticketStepRepository = ticketStepRepository;
    }

    @GetMapping("/workflows")
    public ResponseEntity<?> getWorkflows() {
        try {
            List<EFlowClient.WorkflowSummaryDTO> flows = eFlowClient.getWorkflows(Map.of());
            List<Map<String, Object>> result = new ArrayList<>();
            for (EFlowClient.WorkflowSummaryDTO flow : flows) {
                if (flow == null || flow.id() == null) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>();
                item.put("flowId", flow.id());
                item.put("name", flow.flowName());
                item.put("flowGroup", flow.flowGroup());
                item.put("status", flow.status());
                result.add(item);
            }
            if (!result.isEmpty()) {
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            LOG.warn("Could not fetch workflows from eFlow: {}", e.getMessage());
        }

        return ResponseEntity.ok(
            List.of(
                Map.of("flowId", 1, "name", "Demo Flow 1", "status", "Demo"),
                Map.of("flowId", 2, "name", "Demo Flow 2", "status", "Demo")
            )
        );
    }

    @PostMapping("/ticket/init")
    public ResponseEntity<?> initTicket(HttpServletRequest request, @RequestBody(required = false) InitTicketDTO dto) {
        String creatorEmail = resolveActorEmail(request, DEFAULT_DEMO_CREATOR);
        Long flowId = dto != null && dto.flowId() != null ? dto.flowId() : 1L;
        String ticketName = dto != null && dto.ticketName() != null ? dto.ticketName() : "Demo request";

        Ticket ticket = new Ticket();
        ticket.setFlowId(flowId);
        ticket.setTicketName(ticketName);
        ticket.setCreatorEmail(creatorEmail);
        ticket.setStatus(1);
        ticket.setVersion(1);
        ticket.setCreatedAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());
        ticket = ticketRepository.save(ticket);

        String performerEmail = DEFAULT_DEMO_APPROVER;
        Long firstNodeId = 1L;
        try {
            Map<String, Object> plan = eFlowClient.getFirstActionPlan(flowId);
            if (plan != null && plan.get("nodeId") instanceof Number nodeId) {
                firstNodeId = nodeId.longValue();
            }
            if (plan != null && plan.get("performers") instanceof List<?> performers && !performers.isEmpty()) {
                performerEmail = readPerformer(performers.get(0), DEFAULT_DEMO_APPROVER);
            }
        } catch (Exception e) {
            LOG.warn("Could not fetch first action plan from eFlow for flowId={}: {}", flowId, e.getMessage());
        }

        TicketStep step = new TicketStep();
        step.setTicket(ticket);
        step.setNodeId(firstNodeId);
        step.setPerformerEmail(performerEmail);
        step.setStatus(0);
        step.setStartedAt(Instant.now());
        step = ticketStepRepository.save(step);

        ticket.setCurrentStepId(step.getId());
        ticketRepository.save(ticket);

        Map<String, Object> resp = new HashMap<>();
        resp.put("ticketId", ticket.getId());
        resp.put("status", ticket.getStatus());
        resp.put("performer", performerEmail);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/ticket/related-options")
    public ResponseEntity<?> getRelatedOptions() {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/tickets/my-requests")
    @Transactional
    public ResponseEntity<?> getMyRequests(HttpServletRequest request) {
        String email = resolveActorEmail(request, DEFAULT_DEMO_CREATOR);
        List<Ticket> tickets = ticketRepository.findAllByCreatorEmail(email);
        List<Map<String, Object>> content = new ArrayList<>();
        for (Ticket ticket : tickets) {
            Map<String, Object> item = new HashMap<>();
            item.put("ticketId", ticket.getId());
            item.put("flowId", ticket.getFlowId());
            item.put("ticketName", ticket.getTicketName());
            item.put("flowName", ticket.getTicketName());
            item.put("status", ticket.getStatus());
            item.put("createdDate", ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : null);
            item.put("requesterEmail", ticket.getCreatorEmail());
            content.add(item);
        }
        return ResponseEntity.ok(Map.of("content", content, "totalElements", content.size()));
    }

    @GetMapping("/tickets/pending-tasks")
    @Transactional
    public ResponseEntity<?> getPendingTasks(HttpServletRequest request) {
        String email = resolveActorEmail(request, DEFAULT_DEMO_APPROVER);
        List<TicketStep> steps = ticketStepRepository.findAllByPerformerEmailAndStatus(email, 0);
        List<Map<String, Object>> content = new ArrayList<>();
        for (TicketStep step : steps) {
            Ticket ticket = step.getTicket();
            if (ticket == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("ticketId", ticket.getId());
            item.put("flowId", ticket.getFlowId());
            item.put("flowName", ticket.getTicketName());
            item.put("ticketName", ticket.getTicketName());
            item.put("status", ticket.getStatus());
            item.put("stepId", step.getId());
            item.put("nodeId", step.getNodeId());
            item.put("createdDate", ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : null);
            item.put("requesterEmail", ticket.getCreatorEmail());
            content.add(item);
        }
        return ResponseEntity.ok(Map.of("content", content, "totalElements", content.size()));
    }

    @PostMapping("/ticket/export")
    public ResponseEntity<?> exportTickets() {
        return ResponseEntity.ok(Map.of("message", "Export triggered"));
    }

    @GetMapping("/ticket/{ticketId}/detail")
    public ResponseEntity<?> getTicketDetail(@PathVariable("ticketId") Long ticketId) {
        Optional<Ticket> opt = ticketRepository.findById(ticketId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Ticket ticket = opt.orElseThrow();
        Map<String, Object> resp = new HashMap<>();
        resp.put("ticketId", ticket.getId());
        resp.put("flowId", ticket.getFlowId());
        resp.put("ticketName", ticket.getTicketName());
        resp.put("status", ticket.getStatus());
        resp.put("version", ticket.getVersion());
        resp.put("creatorEmail", ticket.getCreatorEmail());
        resp.put("currentStepId", ticket.getCurrentStepId());
        resp.put("createdAt", ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : null);
        resp.put("completedAt", ticket.getCompletedAt() != null ? ticket.getCompletedAt().toString() : null);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/ticket/{ticketId}/step-config")
    public ResponseEntity<?> getStepConfig(@PathVariable("ticketId") Long ticketId) {
        Optional<Ticket> opt = ticketRepository.findById(ticketId);
        if (opt.isPresent() && opt.orElseThrow().getCurrentStepId() != null) {
            Optional<TicketStep> stepOpt = ticketStepRepository.findById(opt.orElseThrow().getCurrentStepId());
            if (stepOpt.isPresent()) {
                try {
                    return ResponseEntity.ok(eFlowClient.getActionPlan(stepOpt.orElseThrow().getNodeId()));
                } catch (Exception e) {
                    LOG.warn("Could not get action plan: {}", e.getMessage());
                }
            }
        }
        return ResponseEntity.ok(Map.of());
    }

    @PostMapping("/ticket/{ticketId}/submit")
    public ResponseEntity<?> submitTicket(
        HttpServletRequest request,
        @PathVariable("ticketId") Long ticketId,
        @RequestBody SubmitRequestDTO dto
    ) {
        Optional<Ticket> opt = ticketRepository.findById(ticketId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Ticket ticket = opt.orElseThrow();

        if (dto.version() != null && dto.version() < ticket.getVersion()) {
            return ResponseEntity.status(409).body(Map.of("error", "Version conflict"));
        }

        if (dto.formData() != null) {
            try {
                eFormClient.saveFormData(new EFormClient.FormRecordRequestDTO("F_" + ticketId, dto.formData()));
            } catch (Exception e) {
                LOG.warn("Could not save form data: {}", e.getMessage());
            }
        }

        Long currentNodeId = null;
        if (ticket.getCurrentStepId() != null) {
            ticketStepRepository
                .findById(ticket.getCurrentStepId())
                .ifPresent(step -> {
                    step.setStatus(1);
                    step.setFinishedAt(Instant.now());
                    ticketStepRepository.save(step);
                });
            currentNodeId = ticketStepRepository.findById(ticket.getCurrentStepId()).map(TicketStep::getNodeId).orElse(null);
        }

        String nextPerformer = null;
        Long nextNodeId = null;
        if (currentNodeId != null) {
            try {
                Map<String, Object> next = eFlowClient.getNextNode(ticket.getFlowId(), currentNodeId);
                Object nextId = next.get("nextNodeId");
                if (nextId != null) {
                    nextNodeId = Long.valueOf(String.valueOf(nextId));
                    Map<String, Object> plan = eFlowClient.getActionPlan(nextNodeId);
                    if (plan.get("performers") instanceof List<?> performers && !performers.isEmpty()) {
                        nextPerformer = readPerformer(performers.get(0), resolveActorEmail(request, DEFAULT_DEMO_APPROVER));
                    }
                }
            } catch (Exception e) {
                LOG.warn("Could not get next node: {}", e.getMessage());
            }
        }

        if (nextNodeId != null) {
            TicketStep newStep = new TicketStep();
            newStep.setTicket(ticket);
            newStep.setNodeId(nextNodeId);
            newStep.setPerformerEmail(nextPerformer != null ? nextPerformer : resolveActorEmail(request, DEFAULT_DEMO_APPROVER));
            newStep.setStatus(0);
            newStep.setStartedAt(Instant.now());
            newStep = ticketStepRepository.save(newStep);
            ticket.setCurrentStepId(newStep.getId());
        }

        ticket.setVersion(ticket.getVersion() + 1);
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Ticket submitted, moved to next step");
        response.put("performer", nextPerformer);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ticket/{ticketId}/action")
    public ResponseEntity<?> takeAction(@PathVariable("ticketId") Long ticketId, @RequestBody Map<String, Object> payload) {
        Optional<Ticket> opt = ticketRepository.findById(ticketId);
        if (opt.isEmpty()) {
            return ResponseEntity.ok(Map.of("action", payload.get("action"), "status", "Action taken"));
        }

        Ticket ticket = opt.orElseThrow();
        String action = String.valueOf(payload.getOrDefault("action", ""));

        if (ticket.getCurrentStepId() != null) {
            ticketStepRepository
                .findById(ticket.getCurrentStepId())
                .ifPresent(step -> {
                    step.setFinishedAt(Instant.now());
                    if ("APPROVE".equals(action)) {
                        step.setStatus(1);
                    } else if ("REJECT".equals(action)) {
                        step.setStatus(2);
                    }
                    ticketStepRepository.save(step);
                });
        }

        switch (action) {
            case "APPROVE" -> {
                ticket.setStatus(2);
                ticket.setCompletedAt(Instant.now());
            }
            case "REJECT" -> ticket.setStatus(4);
            case "CANCEL" -> ticket.setStatus(3);
            default -> {}
        }
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);

        Map<String, Object> response = new HashMap<>();
        response.put("action", action);
        response.put("status", "Action taken");
        response.put("ticketStatus", ticket.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ticket/{ticketId}/history")
    public ResponseEntity<?> getHistory(@PathVariable("ticketId") Long ticketId) {
        List<TicketStep> steps = ticketStepRepository.findAllByTicketId(ticketId);
        List<Map<String, Object>> history = new ArrayList<>();
        for (TicketStep step : steps) {
            Map<String, Object> item = new HashMap<>();
            item.put("stepId", step.getId());
            item.put("nodeId", step.getNodeId());
            item.put("performerEmail", step.getPerformerEmail());
            item.put("status", step.getStatus());
            item.put("startedAt", step.getStartedAt() != null ? step.getStartedAt().toString() : null);
            item.put("finishedAt", step.getFinishedAt() != null ? step.getFinishedAt().toString() : null);
            history.add(item);
        }
        return ResponseEntity.ok(history);
    }

    @GetMapping("/ticket/{ticketId}/sla")
    public ResponseEntity<?> getSla(@PathVariable("ticketId") Long ticketId) {
        return ResponseEntity.ok(Map.of("remindAt", "2026-10-15T00:00:00Z"));
    }

    @GetMapping("/ticket/{ticketId}/related")
    public ResponseEntity<?> getRelated(@PathVariable("ticketId") Long ticketId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/ticket/{ticketId}/comment")
    public ResponseEntity<?> addComment(@PathVariable("ticketId") Long ticketId, @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(Map.of("status", "Comment added"));
    }

    @PostMapping("/ai/create-from-pdf")
    public ResponseEntity<?> createFromPdf(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of("ticketId", 100));
    }

    @GetMapping("/ai/verify-data")
    public ResponseEntity<?> verifyAiData() {
        return ResponseEntity.ok(Map.of("formData", Map.of("reason", "PDF extracted reason")));
    }

    public record SubmitRequestDTO(Long ticketId, Object formData, Integer version) {}

    public record InitTicketDTO(Long flowId, String ticketName) {}

    private String resolveActorEmail(HttpServletRequest request, String fallbackEmail) {
        String currentUser = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (currentUser != null && !currentUser.isBlank() && !"anonymous".equalsIgnoreCase(currentUser) && !"anonymousUser".equalsIgnoreCase(currentUser)) {
            return currentUser;
        }

        String headerEmail = request.getHeader("X-Demo-User");
        if (headerEmail != null && !headerEmail.isBlank()) {
            return headerEmail.trim();
        }

        String queryEmail = request.getParameter("demoUserEmail");
        if (queryEmail != null && !queryEmail.isBlank()) {
            return queryEmail.trim();
        }

        return fallbackEmail;
    }

    private String readPerformer(Object performerObject, String fallbackEmail) {
        if (performerObject instanceof Map<?, ?> map) {
            Object email = map.get("email");
            if (email != null && !String.valueOf(email).isBlank()) {
                return String.valueOf(email);
            }
            Object userId = map.get("userId");
            if (userId != null && !String.valueOf(userId).isBlank()) {
                return String.valueOf(userId);
            }
        }
        return fallbackEmail;
    }
}

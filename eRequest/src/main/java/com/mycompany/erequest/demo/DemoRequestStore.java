package com.mycompany.erequest.demo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class DemoRequestStore {

    private static final AtomicLong TICKET_SEQ = new AtomicLong(1000);
    private static final AtomicLong STEP_SEQ = new AtomicLong(5000);
    private static final Map<Long, Map<String, Object>> TICKETS = new ConcurrentHashMap<>();
    private static final Map<Long, List<Map<String, Object>>> STEPS = new ConcurrentHashMap<>();

    private DemoRequestStore() {}

    public static Map<String, Object> create(Long flowId, String ticketName, String creatorEmail, String performerEmail, Long nodeId) {
        long ticketId = TICKET_SEQ.incrementAndGet();
        long stepId = STEP_SEQ.incrementAndGet();
        Map<String, Object> ticket = new ConcurrentHashMap<>();
        ticket.put("ticketId", ticketId);
        ticket.put("flowId", flowId);
        ticket.put("ticketName", ticketName);
        ticket.put("creatorEmail", creatorEmail);
        ticket.put("status", 1);
        ticket.put("version", 1);
        ticket.put("currentStepId", stepId);
        ticket.put("createdAt", Instant.now().toString());
        ticket.put("updatedAt", Instant.now().toString());
        TICKETS.put(ticketId, ticket);

        Map<String, Object> step = new ConcurrentHashMap<>();
        step.put("stepId", stepId);
        step.put("nodeId", nodeId);
        step.put("performerEmail", performerEmail);
        step.put("status", 0);
        step.put("startedAt", Instant.now().toString());
        STEPS.put(ticketId, new ArrayList<>(List.of(step)));
        return ticket;
    }

    public static List<Map<String, Object>> myRequests(String creatorEmail) {
        return TICKETS.values().stream().filter(t -> creatorEmail.equals(t.get("creatorEmail"))).toList();
    }

    public static List<Map<String, Object>> pending(String performerEmail) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Map<String, Object>>> entry : STEPS.entrySet()) {
            for (Map<String, Object> step : entry.getValue()) {
                if (performerEmail.equals(step.get("performerEmail")) && Integer.valueOf(0).equals(step.get("status"))) {
                    Map<String, Object> ticket = TICKETS.get(entry.getKey());
                    if (ticket != null) {
                        result.add(ticket);
                    }
                }
            }
        }
        return result;
    }

    public static Map<String, Object> get(Long ticketId) {
        return TICKETS.get(ticketId);
    }

    public static List<Map<String, Object>> history(Long ticketId) {
        return STEPS.getOrDefault(ticketId, List.of());
    }

    public static Map<String, Object> submit(Long ticketId, Long nextNodeId, String nextPerformer) {
        Map<String, Object> ticket = TICKETS.get(ticketId);
        if (ticket == null) {
            return null;
        }
        finishCurrent(ticketId, 1);
        if (nextNodeId != null && nextPerformer != null) {
            long stepId = STEP_SEQ.incrementAndGet();
            Map<String, Object> step = new ConcurrentHashMap<>();
            step.put("stepId", stepId);
            step.put("nodeId", nextNodeId);
            step.put("performerEmail", nextPerformer);
            step.put("status", 0);
            step.put("startedAt", Instant.now().toString());
            STEPS.computeIfAbsent(ticketId, key -> new ArrayList<>()).add(step);
            ticket.put("currentStepId", stepId);
        }
        ticket.put("version", ((Integer) ticket.get("version")) + 1);
        ticket.put("updatedAt", Instant.now().toString());
        return ticket;
    }

    public static Map<String, Object> action(Long ticketId, String action) {
        Map<String, Object> ticket = TICKETS.get(ticketId);
        if (ticket == null) {
            return null;
        }
        if ("APPROVE".equals(action)) {
            finishCurrent(ticketId, 1);
            ticket.put("status", 2);
        } else if ("REJECT".equals(action)) {
            finishCurrent(ticketId, 2);
            ticket.put("status", 4);
        }
        ticket.put("updatedAt", Instant.now().toString());
        return ticket;
    }

    private static void finishCurrent(Long ticketId, int status) {
        List<Map<String, Object>> steps = STEPS.get(ticketId);
        if (steps == null || steps.isEmpty()) {
            return;
        }
        Map<String, Object> current = steps.get(steps.size() - 1);
        current.put("status", status);
        current.put("finishedAt", Instant.now().toString());
    }
}

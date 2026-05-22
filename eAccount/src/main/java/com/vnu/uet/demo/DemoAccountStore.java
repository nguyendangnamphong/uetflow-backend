package com.vnu.uet.demo;

import com.vnu.uet.service.dto.ProfileDTO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class DemoAccountStore {

    private final AtomicLong sequence = new AtomicLong(1000);
    private final ConcurrentHashMap<String, DemoUser> users = new ConcurrentHashMap<>();

    public DemoAccountStore() {
        seedUser(
            demoUser("demo.requester@uetflow.local", "Demo Requester", "0912345601", "Phòng Hành chính", "Nhân viên", "Chuyên viên", "ROLE_USER")
        );
        seedUser(
            demoUser("demo.approver@uetflow.local", "Demo Approver", "0912345602", "Phòng Phê duyệt", "Trưởng phòng", "Quản lý", "ROLE_APPROVER")
        );
        seedUser(
            demoUser("demo.admin@uetflow.local", "Demo Admin", "0912345603", "Phòng CNTT", "Quản trị", "Admin", "ROLE_ADMIN")
        );
        seedUser(
            demoUser("le.van.minh@uetflow.local", "Lê Văn Minh", "0912345604", "Phòng Nhân sự", "Nhân viên", "Chuyên viên", "ROLE_USER")
        );
    }

    public List<Map<String, Object>> listUsers() {
        return users.values().stream().sorted(Comparator.comparing(DemoUser::email)).map(this::toSummary).toList();
    }

    public Map<String, Object> getAccount(String email) {
        return toAccount(getOrDefault(email, "demo.admin@uetflow.local"));
    }

    public Map<String, Object> getProfile(String email) {
        return toProfile(getOrDefault(email, "demo.admin@uetflow.local"));
    }

    public Map<String, Object> getUserDetail(String email) {
        return toDetail(findByEmail(email));
    }

    public Map<String, Object> createUser(ProfileDTO dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());
        DemoUser user = new DemoUser(
            sequence.incrementAndGet(),
            normalizedEmail,
            defaultText(dto.getFirstName(), normalizedEmail),
            dto.getPhone(),
            dto.getDepartment(),
            dto.getPosition(),
            dto.getJob(),
            dto.getGender(),
            dto.getAvatar(),
            dto.getDob(),
            true,
            List.of("ROLE_USER"),
            Instant.now(),
            Instant.now()
        );
        users.put(normalizedEmail, user);
        return toDetail(user);
    }

    public Map<String, Object> updateCurrentUser(String currentEmail, ProfileDTO dto) {
        String normalizedEmail = normalizeEmail(Objects.requireNonNullElse(currentEmail, "demo.admin@uetflow.local"));
        DemoUser updated = users.compute(normalizedEmail, (key, existing) -> mergeUser(key, existing, dto));
        return toDetail(updated);
    }

    public Map<String, Object> searchUser(String email) {
        DemoUser user = findByEmail(email);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("email", user.email());
        response.put("firstName", user.firstName());
        response.put("phone", user.phone());
        response.put("department", user.department());
        response.put("isActive", user.activated());
        response.put("roles", user.roles());
        return response;
    }

    private DemoUser mergeUser(String key, DemoUser existing, ProfileDTO dto) {
        DemoUser base =
            existing != null ? existing : new DemoUser(sequence.incrementAndGet(), key, key, "0900000000", "Phòng demo", null, null, null, null, null, true, List.of("ROLE_USER"), Instant.now(), Instant.now());
        return new DemoUser(
            base.id(),
            base.email(),
            defaultText(dto.getFirstName(), base.firstName()),
            defaultText(dto.getPhone(), base.phone()),
            defaultText(dto.getDepartment(), base.department()),
            defaultText(dto.getPosition(), base.position()),
            defaultText(dto.getJob(), base.job()),
            defaultText(dto.getGender(), base.gender()),
            defaultText(dto.getAvatar(), base.avatar()),
            dto.getDob() != null ? dto.getDob() : base.dob(),
            base.activated(),
            base.roles(),
            base.createdDate(),
            Instant.now()
        );
    }

    private DemoUser getOrDefault(String email, String fallbackEmail) {
        String normalizedEmail = normalizeEmail(email);
        return users.getOrDefault(normalizedEmail, users.get(fallbackEmail));
    }

    private DemoUser findByEmail(String email) {
        DemoUser user = users.get(normalizeEmail(email));
        if (user == null) {
            throw new DemoNotFoundException("Demo user not found: " + email);
        }
        return user;
    }

    private void seedUser(DemoUser user) {
        users.put(user.email(), user);
    }

    private DemoUser demoUser(
        String email,
        String firstName,
        String phone,
        String department,
        String position,
        String job,
        String role
    ) {
        Instant createdDate = Instant.now().minus(5, ChronoUnit.DAYS);
        return new DemoUser(
            sequence.incrementAndGet(),
            email,
            firstName,
            phone,
            department,
            position,
            job,
            "UNKNOWN",
            "",
            null,
            true,
            List.of(role),
            createdDate,
            createdDate.plus(1, ChronoUnit.HOURS)
        );
    }

    private Map<String, Object> toSummary(DemoUser user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.id());
        map.put("email", user.email());
        map.put("firstName", user.firstName());
        map.put("phone", user.phone());
        map.put("department", user.department());
        map.put("position", user.position());
        map.put("job", user.job());
        map.put("activated", user.activated());
        map.put("roles", user.roles());
        return map;
    }

    private Map<String, Object> toProfile(DemoUser user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("email", user.email());
        map.put("firstName", user.firstName());
        map.put("phone", user.phone());
        map.put("dob", user.dob());
        map.put("gender", user.gender());
        map.put("position", user.position());
        map.put("job", user.job());
        map.put("department", user.department());
        map.put("avatar", user.avatar());
        map.put("roles", user.roles());
        return map;
    }

    private Map<String, Object> toAccount(DemoUser user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.id());
        response.put("login", user.email());
        response.put("firstName", user.firstName());
        response.put("lastName", "");
        response.put("email", user.email());
        response.put("gender", user.gender());
        response.put("address", "Khu demo UETFlow");
        response.put("imageUrl", user.avatar());
        response.put("activated", user.activated());
        response.put("langKey", "vi");
        response.put("createdBy", "demo-mode");
        response.put("createdDate", user.createdDate());
        response.put("lastModifiedBy", "demo-mode");
        response.put("lastModifiedDate", user.lastModifiedDate());
        response.put("authorities", user.roles());
        response.put("phone", user.phone());
        response.put("dob", user.dob());
        response.put("avatar", user.avatar());
        response.put("signature", null);
        response.put("custId", 1);
        response.put("orgName", "UETFlow Demo");
        response.put("custType", "O");
        response.put("parentOrgName", "UETFlow Demo");
        response.put("taxCodeOrg", "DEMO-0001");
        response.put("acNameOrg", "UETFLOW DEMO");
        response.put("orgCode", "DEMO");
        response.put("organization", Map.of("id", 1, "name", "UETFlow Demo", "custId", 1, "activated", true));
        response.put("subOrganizations", List.of());
        response.put("step", null);
        response.put("otherDataStorage", Map.of("id", 1, "type", "demo", "content", "demo"));
        response.put("activityLogName", null);
        response.put("groupId", null);
        response.put("userId", user.id());
        response.put("emailCustomer", user.email());
        response.put("features", List.of("demo"));
        response.put("orgIn", "/demo/1");
        response.put("dbSuffix", "demo");
        response.put("blocked", false);
        response.put("folderPath", "/demo/storage");
        response.put("folderId", "demo-folder");
        response.put("orgId", 1);
        response.put("digitalSignature", Map.of("name", "Chữ ký số demo", "type", "TB", "star", true));
        response.put("defaultOrgIn", null);
        response.put("bio", null);
        response.put("taxCode", null);
        response.put("knowledgeUsers", null);
        response.put("attrs", Map.of("priority", List.of(Map.of("orgIn", "/demo/1", "priority", user.id()))));
        response.put("existedIdCard", false);
        response.put("lastModifiedPasswordDate", user.lastModifiedDate());
        response.put("roles", user.roles());
        response.put("policiesIdList", null);
        response.put("policiesList", null);
        response.put("userDTOAuthor", null);
        response.put("legalType", null);
        response.put("zoom", "1");
        response.put("groups", List.of());
        return response;
    }

    private Map<String, Object> toDetail(DemoUser user) {
        Map<String, Object> map = new LinkedHashMap<>(toProfile(user));
        map.put("id", user.id());
        map.put("activated", user.activated());
        map.put("createdDate", user.createdDate());
        map.put("lastModifiedDate", user.lastModifiedDate());
        return map;
    }

    private String normalizeEmail(String email) {
        return Objects.requireNonNullElse(email, "demo.admin@uetflow.local").trim().toLowerCase();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static class DemoNotFoundException extends RuntimeException {
        public DemoNotFoundException(String message) {
            super(message);
        }
    }

    private record DemoUser(
        long id,
        String email,
        String firstName,
        String phone,
        String department,
        String position,
        String job,
        String gender,
        String avatar,
        Instant dob,
        boolean activated,
        List<String> roles,
        Instant createdDate,
        Instant lastModifiedDate
    ) {}
}

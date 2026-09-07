package com.vijay.todo_management.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.vijay.todo_management.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodoUpdateRequest {

    private String title;
    private JsonNode descriptionJson;
    private String descriptionPlainText;
    private Priority priority;
    private Set<String> tagNames;

    @JsonIgnore
    private boolean descriptionJsonPresent = false;

    @JsonIgnore
    private boolean descriptionPlainTextPresent = false;

    public void setDescriptionJson(JsonNode descriptionJson) {
        this.descriptionJson = descriptionJson;
        this.descriptionJsonPresent = true;
    }

    public void setDescriptionPlainText(String descriptionPlainText) {
        this.descriptionPlainText = descriptionPlainText;
        this.descriptionPlainTextPresent = true;
    }

    @JsonIgnore
    public boolean isDescriptionUpdateRequested() {
        return descriptionJsonPresent || descriptionPlainTextPresent;
    }

    // ── Scheduling (validated: startDateTime must not be after endDateTime) ──
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;  // replaces dueDate

    // ── Effort / Planning ────────────────────────────────────────────────────
    private BigDecimal estimatedTime;   // decimal hours
    private BigDecimal remainingTime;   // decimal hours
    private Integer storyPoints;
}

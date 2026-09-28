package com.proitbridge.lms.web;

import com.proitbridge.lms.security.CurrentUser;
import com.proitbridge.lms.service.StudyTimeService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Mentor check-in/check-out and activity heartbeat. */
@RestController
@RequestMapping("/api/staff/study")
public class StaffStudyController {
    private final StudyTimeService study;
    private final CurrentUser current;

    public StaffStudyController(StudyTimeService study, CurrentUser current) {
        this.study = study;
        this.current = current;
    }

    @GetMapping
    public Map<String, Object> summary() {
        var me = current.get();
        return study.staffCheckInStatus(me.id());
    }

    @PostMapping("/check-in")
    public Map<String, Object> checkIn(@RequestBody(required = false) Map<String, String> body) {
        var me = current.get();
        return study.staffCheckIn(me.id(), me.role(), body == null ? null : body.get("note"));
    }

    @PostMapping("/heartbeat")
    public Map<String, Object> heartbeat() {
        return study.staffHeartbeat(current.get().id());
    }

    @PostMapping("/check-out")
    public Map<String, Object> checkOut() {
        return study.staffCheckOut(current.get().id());
    }
}

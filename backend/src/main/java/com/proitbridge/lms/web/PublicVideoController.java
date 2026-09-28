package com.proitbridge.lms.web;

import com.proitbridge.lms.service.VideoAccessService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Authenticated video library visible to every LMS role. */
@RestController
@RequestMapping("/api/videos")
public class PublicVideoController {
    private final VideoAccessService videos;

    public PublicVideoController(VideoAccessService videos) {
        this.videos = videos;
    }

    @GetMapping
    public List<Map<String, Object>> library() {
        return videos.library().stream().filter(v -> "LIBRARY".equals(v.get("kind"))).collect(Collectors.toList());
    }
}

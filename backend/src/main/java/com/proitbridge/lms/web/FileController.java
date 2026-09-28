package com.proitbridge.lms.web;

import com.proitbridge.lms.domain.StoredFile;
import com.proitbridge.lms.security.CurrentUser;
import com.proitbridge.lms.service.FileService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Uploads and downloads. Both sides are authenticated; nothing is served publicly. */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService files;
    private final CurrentUser current;

    public FileController(FileService files, CurrentUser current) {
        this.files = files;
        this.current = current;
    }

    @PostMapping
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "TASK") String purpose,
                                      @RequestParam(required = false) String linkedId) {
        StoredFile f = files.store(file, current.get().id(), purpose, linkedId);
        return Map.of("id", f.getId(), "filename", f.getFilename(),
                "sizeBytes", f.getSizeBytes(), "contentType", f.getContentType());
    }

    /**
     * Being signed in was the whole check, which is not a check at all: a file id read
     * off a task thread downloaded another learner's notebook. Staff still get
     * everything, because reviewing work is the job. A learner gets their own.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String id) {
        StoredFile f = files.meta(id);
        var me = current.get();
        if ("LEARNER".equals(me.role()) && !me.id().equals(f.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That file is not yours.");
        }
        byte[] bytes = files.read(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + f.getFilename().replace("\"", "") + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }
}

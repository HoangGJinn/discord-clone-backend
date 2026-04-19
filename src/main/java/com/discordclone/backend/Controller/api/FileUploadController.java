package com.discordclone.backend.Controller.api;

import com.discordclone.backend.service.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FileUploadController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "resourceType", required = false, defaultValue = "auto") String resourceType) {
        System.out.println("DEBUG: Nhận yêu cầu upload file: " + file.getOriginalFilename());
        try {
            Map<?, ?> result = fileService.upload(file);
            String url = (String) result.get("secure_url");
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            
            Map<String, Object> response = new HashMap<>();
            response.put("url", url);
            response.put("filename", filename != null ? filename : "file");
            response.put("contentType", contentType != null ? contentType : "application/octet-stream");
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Upload rejected: " + e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        }
    }
}

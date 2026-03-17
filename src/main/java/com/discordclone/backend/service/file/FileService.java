package com.discordclone.backend.service.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface FileService {
    Map upload(MultipartFile file) throws IOException;
}

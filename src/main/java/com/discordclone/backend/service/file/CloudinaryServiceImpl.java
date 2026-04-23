package com.discordclone.backend.service.file;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements FileService {

    private final Cloudinary cloudinary;

    @Override
    public Map<String, Object> upload(MultipartFile file) throws IOException {
        return upload(file, "auto");
    }

    @Override
    public Map<String, Object> upload(MultipartFile file, String resourceType) throws IOException {
        String normalizedType = (resourceType == null || resourceType.isBlank()) ? "auto" : resourceType;
        return cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", normalizedType,
                        "use_filename", true,
                        "unique_filename", false,
                        "filename_override", file.getOriginalFilename()
                )
        );
    }
}

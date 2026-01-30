package com.discordclone.backend.service.category;

import com.discordclone.backend.dto.request.CreateCategoryRequest;
import com.discordclone.backend.dto.request.UpdateCategoryRequest;
import com.discordclone.backend.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    // Tạo category mới
    CategoryResponse createCategory(CreateCategoryRequest request, Long userId);

    // Lấy thông tin category theo ID
    CategoryResponse getCategoryById(Long categoryId);

    // Cập nhật category
    CategoryResponse updateCategory(Long categoryId, UpdateCategoryRequest request, Long userId);

    // Xóa category
    void deleteCategory(Long categoryId, Long userId);

    // Lấy danh sách categories của server
    List<CategoryResponse> getCategoriesByServer(Long serverId);
}

package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.request.CreateCategoryRequest;
import com.discordclone.backend.dto.request.UpdateCategoryRequest;
import com.discordclone.backend.dto.response.CategoryResponse;
import com.discordclone.backend.security.services.UserDetailsImpl;
import com.discordclone.backend.service.category.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
public class CategoryController {

    private final CategoryService categoryService;

    // Tạo category mới - chỉ OWNER hoặc ADMIN của server
    @PostMapping("/servers/{serverId}/categories")
    @PreAuthorize("@serverSecurity.isAdmin(#serverId, principal.id)")
    public ResponseEntity<CategoryResponse> createCategory(
            @PathVariable Long serverId,
            @Valid @RequestBody CreateCategoryRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        request.setServerId(serverId);
        CategoryResponse response = categoryService.createCategory(request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    // Lấy danh sách categories - chỉ member mới xem được
    @GetMapping("/servers/{serverId}/categories")
    @PreAuthorize("@serverSecurity.isMember(#serverId, principal.id)")
    public ResponseEntity<List<CategoryResponse>> getCategoriesByServer(@PathVariable Long serverId) {
        List<CategoryResponse> categories = categoryService.getCategoriesByServer(serverId);
        return ResponseEntity.ok(categories);
    }

    // Lấy thông tin category - chỉ member của server mới xem được
    @GetMapping("/categories/{categoryId}")
    @PreAuthorize("@serverSecurity.isMemberOfCategory(#categoryId, principal.id)")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long categoryId) {
        CategoryResponse response = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(response);
    }

    // Cập nhật category - chỉ OWNER hoặc ADMIN
    @PutMapping("/categories/{categoryId}")
    @PreAuthorize("@serverSecurity.isAdminOfCategory(#categoryId, principal.id)")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CategoryResponse response = categoryService.updateCategory(categoryId, request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    // Xóa category - chỉ OWNER hoặc ADMIN
    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("@serverSecurity.isAdminOfCategory(#categoryId, principal.id)")
    public ResponseEntity<Map<String, String>> deleteCategory(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        categoryService.deleteCategory(categoryId, userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "Xóa category thành công"));
    }
}

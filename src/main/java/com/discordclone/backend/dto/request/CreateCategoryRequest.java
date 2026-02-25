package com.discordclone.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {

    @NotBlank(message = "Tên category không được để trống")
    @Size(min = 1, max = 100, message = "Tên category phải từ 1-100 ký tự")
    private String name;

    @NotNull(message = "Server ID không được để trống")
    private Long serverId;
}

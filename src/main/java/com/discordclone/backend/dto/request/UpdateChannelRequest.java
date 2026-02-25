package com.discordclone.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChannelRequest {

    @Size(min = 1, max = 100, message = "Tên channel phải từ 1-100 ký tự")
    private String name;

    @Size(max = 500, message = "Topic không được quá 500 ký tự")
    private String topic;

    private Integer position;

    // Cho phép chuyển channel sang category khác
    private Long categoryId;
}

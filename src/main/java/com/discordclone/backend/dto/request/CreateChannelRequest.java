package com.discordclone.backend.dto.request;

import com.discordclone.backend.entity.enums.ChannelType;
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
public class CreateChannelRequest {

    @NotBlank(message = "Tên channel không được để trống")
    @Size(min = 1, max = 100, message = "Tên channel phải từ 1-100 ký tự")
    private String name;

    @NotNull(message = "Server ID không được để trống")
    private Long serverId;

    // Optional - nếu null thì channel nằm trực tiếp trong server
    private Long categoryId;

    @Builder.Default
    private ChannelType type = ChannelType.TEXT;

    @Size(max = 500, message = "Topic không được quá 500 ký tự")
    private String topic;
}

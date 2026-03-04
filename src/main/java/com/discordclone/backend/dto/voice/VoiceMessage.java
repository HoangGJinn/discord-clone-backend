package com.discordclone.backend.dto.voice;

import com.discordclone.backend.entity.enums.VoiceMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceMessage {
    private VoiceMessageType type;    // Loại sự kiện (JOIN, LEAVE...)
    private VoiceState state;         // Trạng thái của user vừa thao tác
    private List<VoiceState> states;  // Danh sách user (Dành cho INITIAL_SYNC)
}

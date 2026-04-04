package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.voice.VoiceState;
import com.discordclone.backend.entity.enums.ChannelType;
import com.discordclone.backend.entity.jpa.Channel;
import com.discordclone.backend.repository.ChannelRepository;
import com.discordclone.backend.service.voice.VoiceStateService;
import com.discordclone.backend.utils.agora.RtcTokenBuilder2;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class VoiceController {

    @Value("${agora.app-id}")
    private String appId;

    @Value("${agora.app-certificate}")
    private String appCertificate;

    // Thời gian Token hết hạn: 24h = 86400 giây
    private final int TOKEN_EXPIRE_TIME = 86400;

    private final ChannelRepository channelRepository;
    private final VoiceStateService voiceStateService;

    @GetMapping("/token")
    public ResponseEntity<?> getVoiceToken(
            @RequestParam String channelId,
            @RequestParam String userId) {

        try {
            Long parsedChannelId = Long.parseLong(channelId);
            Optional<Channel> optionalChannel = channelRepository.findById(parsedChannelId);
            if (optionalChannel.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Kênh không tồn tại!");
            }
            Channel channel = optionalChannel.get();

            if (channel.getType() != ChannelType.VOICE) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Đây không phải là kênh thoại!");
            }

            if (channel.getUserLimit() != null && channel.getUserLimit() > 0) {
                List<VoiceState> currentUsers = voiceStateService.getStatesByChannel(parsedChannelId);
                boolean isAlreadyInRoom = currentUsers.stream()
                        .anyMatch(state -> state.getUserId().equals(userId));
                if (!isAlreadyInRoom && currentUsers.size() >= channel.getUserLimit()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Phòng thoại đã đầy!");
                }
            }

            // Nếu không có certificate (Testing Mode) → trả token rỗng
            String resultToken = "";
            if (appCertificate != null && !appCertificate.isBlank()) {
                RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();
                resultToken = tokenBuilder.buildTokenWithUserAccount(
                        appId,
                        appCertificate,
                        channelId,
                        userId,
                        RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                        TOKEN_EXPIRE_TIME,
                        TOKEN_EXPIRE_TIME);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("token", resultToken);
            response.put("channelId", channelId);
            response.put("userId", userId);
            response.put("bitrate", channel.getBitrate());
            response.put("userLimit", channel.getUserLimit());
            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Định dạng Channel ID không hợp lệ");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi khi tạo Token: " + e.getMessage());
        }
    }
}

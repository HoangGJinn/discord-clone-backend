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
            // 1. Kiểm tra xem channel có tồn tại trong Database không
            Optional<Channel> optionalChannel = channelRepository.findById(parsedChannelId);
            if (optionalChannel.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Kênh không tồn tại!");
            }
            Channel channel = optionalChannel.get();

            // 2. Kiểm tra xem đây có phải là loại kênh Voice hợp lệ không
            if (channel.getType() != ChannelType.VOICE) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Đây không phải là kênh thoại!");
            }

            // 3. Kiểm tra Giới hạn người (User Limit) - 0 nghĩa là không giới hạn
            if (channel.getUserLimit() != null && channel.getUserLimit() > 0) {
                // Đếm số người hiện đang ở trong phòng bằng VoiceStateService
                List<VoiceState> currentUsers = voiceStateService.getStatesByChannel(parsedChannelId);
                // Trừ hao nếu user này đang Join lại hoặc bị rớt mạng nhưng server chưa kịp xóa
                // thì không bị tính là người thứ n+1
                boolean isAlreadyInRoom = currentUsers.stream()
                        .anyMatch(state -> state.getUserId().equals(userId));
                if (!isAlreadyInRoom && currentUsers.size() >= channel.getUserLimit()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Phòng thoại đã đầy!");
                }
            }

            // 4. Nếu qua hết các bài test, tiến hành cấp thẻ (Token)
            RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();
            String resultToken = tokenBuilder.buildTokenWithUserAccount(
                    appId,
                    appCertificate,
                    channelId,
                    userId,
                    RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                    TOKEN_EXPIRE_TIME,
                    TOKEN_EXPIRE_TIME);
            Map<String, Object> response = new HashMap<>(); // Đổi sang Object để chứa nhiều loại kiểu dữ liệu
            response.put("token", resultToken);
            response.put("channelId", channelId);
            response.put("userId", userId);

            // Có thể trả thêm cấu hình phòng cho Client chủ động hiển thị
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

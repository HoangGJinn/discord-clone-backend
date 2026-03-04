package com.discordclone.backend.entity.enums;

public enum VoiceMessageType {
    JOIN,           // Khi người dùng tham gia kênh thoại
    LEAVE,          // Khi người dùng rời kênh thoại
    UPDATE_STATE,   // Khi người dùng bật/tắt mic hoặc tai nghe
    INITIAL_SYNC    // Dùng để server gửi toàn bộ danh sách hiện tại cho người mới vào phòng
}

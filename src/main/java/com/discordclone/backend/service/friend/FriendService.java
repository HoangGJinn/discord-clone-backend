package com.discordclone.backend.service.friend;

import com.discordclone.backend.dto.response.FriendshipResponse;
import com.discordclone.backend.dto.response.UserSearchResponse;

import java.util.List;

public interface FriendService {

    /** Tìm kiếm user theo username hoặc displayName */
    List<UserSearchResponse> searchUsers(String keyword, Long currentUserId);

    /** Tìm kiếm bạn bè theo username hoặc displayName */
    List<UserSearchResponse> searchFriends(String keyword, Long currentUserId);

    /** Gửi lời mời kết bạn */
    FriendshipResponse sendFriendRequest(Long senderId, Long receiverId);

    /** Chấp nhận lời mời kết bạn */
    FriendshipResponse acceptFriendRequest(Long friendshipId, Long currentUserId);

    /** Từ chối lời mời kết bạn */
    FriendshipResponse rejectFriendRequest(Long friendshipId, Long currentUserId);

    /** Hủy lời mời đã gửi (người gửi tự hủy) */
    void cancelFriendRequest(Long friendshipId, Long currentUserId);

    /** Xóa bạn bè */
    void unfriend(Long friendshipId, Long currentUserId);

    /** Block user */
    FriendshipResponse blockUser(Long targetUserId, Long currentUserId);

    /** Danh sách bạn bè đã ACCEPTED */
    List<FriendshipResponse> getFriends(Long userId);

    /** Danh sách lời mời ĐANG CHỜ nhận được */
    List<FriendshipResponse> getPendingRequests(Long userId);

    /** Danh sách lời mời ĐÃ GỬI đang chờ */
    List<FriendshipResponse> getSentRequests(Long userId);
}

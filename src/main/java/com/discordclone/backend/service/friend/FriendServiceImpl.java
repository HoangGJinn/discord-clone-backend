package com.discordclone.backend.service.friend;

import com.discordclone.backend.dto.response.FriendshipResponse;
import com.discordclone.backend.dto.response.UserSearchResponse;
import com.discordclone.backend.entity.enums.FriendshipStatus;
import com.discordclone.backend.entity.jpa.Friendship;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.exception.ResourceNotFoundException;
import com.discordclone.backend.repository.FriendshipRepository;
import com.discordclone.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendServiceImpl implements FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    // ─── Tìm kiếm user ─────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(String keyword, Long currentUserId) {
        if (keyword == null || keyword.trim().length() < 2) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm phải có ít nhất 2 ký tự");
        }

        String trimmed = keyword.trim();

        // Dùng JPQL query từ UserRepository (case-insensitive, hiệu quả hơn findAll)
        List<User> users = userRepository.searchByKeyword(trimmed).stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .collect(Collectors.toList());

        return users.stream()
                .map(u -> buildUserSearchResponse(u, currentUserId))
                .collect(Collectors.toList());
    }

    // ─── Gửi lời mời ───────────────────────────────────────────────────────────
    @Override
    public FriendshipResponse sendFriendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Không thể gửi lời mời cho chính mình");
        }

        User sender = getUserById(senderId);
        User receiver = getUserById(receiverId);

        // Kiểm tra đã có mối quan hệ chưa
        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(senderId, receiverId);
        if (existing.isPresent()) {
            FriendshipStatus status = existing.get().getStatus();
            switch (status) {
                case ACCEPTED:
                    throw new IllegalStateException("Hai người đã là bạn bè");
                case PENDING:
                    throw new IllegalStateException("Lời mời kết bạn đã được gửi, đang chờ xác nhận");
                case BLOCKED:
                    throw new IllegalStateException("Không thể gửi lời mời do bị chặn");
                case REJECTED:
                    // Cho phép gửi lại sau khi bị từ chối — reset về PENDING
                    Friendship old = existing.get();
                    old.setStatus(FriendshipStatus.PENDING);
                    // Đặt người gửi hiện tại làm sender
                    old.setSender(sender);
                    old.setReceiver(receiver);
                    return FriendshipResponse.from(friendshipRepository.save(old));
            }
        }

        Friendship friendship = Friendship.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendshipStatus.PENDING)
                .build();

        return FriendshipResponse.from(friendshipRepository.save(friendship));
    }

    // ─── Chấp nhận lời mời ─────────────────────────────────────────────────────
    @Override
    public FriendshipResponse acceptFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship f = getFriendshipById(friendshipId);

        // Chỉ người nhận mới được chấp nhận
        if (!f.getReceiver().getId().equals(currentUserId)) {
            throw new IllegalStateException("Bạn không có quyền chấp nhận lời mời này");
        }
        if (f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Lời mời không ở trạng thái chờ xác nhận");
        }

        f.setStatus(FriendshipStatus.ACCEPTED);
        return FriendshipResponse.from(friendshipRepository.save(f));
    }

    // ─── Từ chối lời mời ───────────────────────────────────────────────────────
    @Override
    public FriendshipResponse rejectFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship f = getFriendshipById(friendshipId);

        // Chỉ người nhận mới được từ chối
        if (!f.getReceiver().getId().equals(currentUserId)) {
            throw new IllegalStateException("Bạn không có quyền từ chối lời mời này");
        }
        if (f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Lời mời không ở trạng thái chờ xác nhận");
        }

        f.setStatus(FriendshipStatus.REJECTED);
        return FriendshipResponse.from(friendshipRepository.save(f));
    }

    // ─── Hủy lời mời đã gửi ───────────────────────────────────────────────────
    @Override
    public void cancelFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship f = getFriendshipById(friendshipId);

        if (!f.getSender().getId().equals(currentUserId)) {
            throw new IllegalStateException("Bạn không có quyền hủy lời mời này");
        }
        if (f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy lời mời đang chờ xác nhận");
        }

        friendshipRepository.delete(f);
    }

    // ─── Xóa bạn bè ────────────────────────────────────────────────────────────
    @Override
    public void unfriend(Long friendshipId, Long currentUserId) {
        Friendship f = getFriendshipById(friendshipId);

        boolean isParticipant = f.getSender().getId().equals(currentUserId)
                || f.getReceiver().getId().equals(currentUserId);

        if (!isParticipant) {
            throw new IllegalStateException("Bạn không có quyền thực hiện hành động này");
        }
        if (f.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new IllegalStateException("Hai người không phải bạn bè");
        }

        friendshipRepository.delete(f);
    }

    // ─── Block user ─────────────────────────────────────────────────────────────
    @Override
    public FriendshipResponse blockUser(Long targetUserId, Long currentUserId) {
        User blocker = getUserById(currentUserId);
        User target = getUserById(targetUserId);

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(currentUserId, targetUserId);

        Friendship f;
        if (existing.isPresent()) {
            f = existing.get();
        } else {
            f = new Friendship();
        }

        f.setSender(blocker);
        f.setReceiver(target);
        f.setStatus(FriendshipStatus.BLOCKED);

        return FriendshipResponse.from(friendshipRepository.save(f));
    }

    // ─── Danh sách bạn bè ──────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getFriends(Long userId) {
        return friendshipRepository.findAllFriendsOf(userId).stream()
                .map(FriendshipResponse::from)
                .collect(Collectors.toList());
    }

    // ─── Lời mời nhận được ─────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getPendingRequests(Long userId) {
        return friendshipRepository.findByReceiverIdAndStatus(userId, FriendshipStatus.PENDING).stream()
                .map(FriendshipResponse::from)
                .collect(Collectors.toList());
    }

    // ─── Lời mời đã gửi ────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getSentRequests(Long userId) {
        return friendshipRepository.findBySenderIdAndStatus(userId, FriendshipStatus.PENDING).stream()
                .map(FriendshipResponse::from)
                .collect(Collectors.toList());
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại: " + id));
    }

    private Friendship getFriendshipById(Long id) {
        return friendshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mối quan hệ bạn bè"));
    }

    private boolean matchKeyword(User u, String keyword) {
        boolean matchUsername = u.getUserName() != null
                && u.getUserName().toLowerCase().contains(keyword);
        boolean matchDisplay = u.getDisplayName() != null
                && u.getDisplayName().toLowerCase().contains(keyword);
        return matchUsername || matchDisplay;
    }

    private UserSearchResponse buildUserSearchResponse(User user, Long currentUserId) {
        Optional<Friendship> rel = friendshipRepository.findBetweenUsers(currentUserId, user.getId());

        String status = null;
        Long friendshipId = null;
        Boolean isSender = null;

        if (rel.isPresent()) {
            Friendship f = rel.get();
            status = f.getStatus().name();
            friendshipId = f.getId();
            isSender = f.getSender().getId().equals(currentUserId);
        }

        return UserSearchResponse.builder()
                .id(user.getId())
                .username(user.getUserName())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .friendshipStatus(status)
                .friendshipId(friendshipId)
                .isSender(isSender)
                .build();
    }
}

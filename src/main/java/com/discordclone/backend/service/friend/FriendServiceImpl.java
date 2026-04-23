package com.discordclone.backend.service.friend;

import com.discordclone.backend.dto.response.FriendshipResponse;
import com.discordclone.backend.dto.response.UserSearchResponse;
import com.discordclone.backend.entity.enums.FriendshipStatus;
import com.discordclone.backend.entity.jpa.Friendship;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.exception.ResourceNotFoundException;
import com.discordclone.backend.repository.FriendshipRepository;
import com.discordclone.backend.repository.UserFcmTokenRepository;
import com.discordclone.backend.repository.UserRepository;
import com.discordclone.backend.service.impl.FcmService;
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
    private final UserFcmTokenRepository fcmTokenRepository;
    private final FcmService fcmService;

    // --- Search users ---
    @Override
    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(String keyword, Long currentUserId) {
        if (keyword == null || keyword.trim().length() < 2) {
            throw new IllegalArgumentException("Search keyword must have at least 2 characters");
        }

        String trimmed = keyword.trim();

        // Use JPQL query from UserRepository (case-insensitive, more efficient than findAll)
        List<User> users = userRepository.searchByKeyword(trimmed).stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .collect(Collectors.toList());

        return users.stream()
                .map(u -> buildUserSearchResponse(u, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchFriends(String keyword, Long currentUserId) {
        if (keyword == null || keyword.trim().length() < 2) {
            throw new IllegalArgumentException("Keyword must be at least 2 characters");
        }

        String trimmed = keyword.trim();

        // Get list of accepted friendships
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(currentUserId);

        return friendships.stream()
                .map(f -> f.getSender().getId().equals(currentUserId) ? f.getReceiver() : f.getSender())
                .filter(u -> {
                    String username = u.getUserName() == null ? "" : u.getUserName().toLowerCase();
                    String displayName = u.getDisplayName() == null ? "" : u.getDisplayName().toLowerCase();
                    return username.contains(trimmed.toLowerCase()) || displayName.contains(trimmed.toLowerCase());
                })
                .map(u -> buildUserSearchResponse(u, currentUserId))
                .collect(Collectors.toList());
    }

    // --- Send friend request ---
    @Override
    public FriendshipResponse sendFriendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot send a friend request to yourself");
        }

        User sender = getUserById(senderId);
        User receiver = getUserById(receiverId);

        // Check if friendship already exists
        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(senderId, receiverId);
        if (existing.isPresent()) {
            FriendshipStatus status = existing.get().getStatus();
            switch (status) {
                case ACCEPTED:
                    throw new IllegalArgumentException("You are already friends");
                case PENDING:
                    throw new IllegalArgumentException("Friend request already sent, waiting for confirmation");
                case BLOCKED:
                    throw new IllegalArgumentException("Cannot send a friend request because you are blocked");
                case REJECTED:
                    // Allow sending again after being rejected - reset to PENDING
                    Friendship old = existing.get();
                    old.setStatus(FriendshipStatus.PENDING);
                    // Set current sender as sender
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

        FriendshipResponse saved = FriendshipResponse.from(friendshipRepository.save(friendship));

        // Gửi FCM notification đến người nhận lời mời kết bạn
        try {
            List<String> tokens = fcmTokenRepository.findFcmTokensByUserId(receiverId);
            if (!tokens.isEmpty()) {
                String senderName = sender.getDisplayName() != null
                        ? sender.getDisplayName() : sender.getUserName();
                fcmService.sendFriendRequestNotification(
                        tokens,
                        senderName,
                        String.valueOf(senderId),
                        String.valueOf(saved.getId())
                );
            }
        } catch (Exception e) {
            System.err.println("[FCM] sendFriendRequestNotification failed: " + e.getMessage());
        }

        return saved;
    }

    // --- Accept friend request ---
    @Override
    public FriendshipResponse acceptFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship f = getFriendshipById(friendshipId);

        // Only receiver can accept
        if (!f.getReceiver().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("You don't have permission to accept this request");
        }
        if (f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Friend request is not in pending state");
        }

        f.setStatus(FriendshipStatus.ACCEPTED);
        return FriendshipResponse.from(friendshipRepository.save(f));
    }

    // --- Reject friend request ---
    @Override
    public FriendshipResponse rejectFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship f = getFriendshipById(friendshipId);

        // Only receiver can reject
        if (!f.getReceiver().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("You don't have permission to reject this request");
        }
        if (f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Friend request is not in pending state");
        }

        f.setStatus(FriendshipStatus.REJECTED);
        return FriendshipResponse.from(friendshipRepository.save(f));
    }

    // --- Cancel sent friend request ---
    @Override
    public void cancelFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship f = getFriendshipById(friendshipId);

        if (!f.getSender().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("You don't have permission to cancel this request");
        }
        if (f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Can only cancel pending requests");
        }

        friendshipRepository.delete(f);
    }

    // --- Unfriend ---
    @Override
    public void unfriend(Long friendshipId, Long currentUserId) {
        Friendship f = getFriendshipById(friendshipId);

        boolean isParticipant = f.getSender().getId().equals(currentUserId)
                || f.getReceiver().getId().equals(currentUserId);

        if (!isParticipant) {
            throw new IllegalArgumentException("You don't have permission to perform this action");
        }
        if (f.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new IllegalArgumentException("You are not friends");
        }

        friendshipRepository.delete(f);
    }

    // --- Block user ---
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

    // --- Get friends list ---
    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getFriends(Long userId) {
        return friendshipRepository.findAllFriendsOf(userId).stream()
                .map(FriendshipResponse::from)
                .collect(Collectors.toList());
    }

    // â”€â”€â”€ Lá»i má»i nháº­n Ä‘Æ°á»£c â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getPendingRequests(Long userId) {
        return friendshipRepository.findByReceiverIdAndStatus(userId, FriendshipStatus.PENDING).stream()
                .map(FriendshipResponse::from)
                .collect(Collectors.toList());
    }

    // â”€â”€â”€ Lá»i má»i Ä‘Ã£ gá»­i â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getSentRequests(Long userId) {
        return friendshipRepository.findBySenderIdAndStatus(userId, FriendshipStatus.PENDING).stream()
                .map(FriendshipResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserSearchResponse getFriendshipStatus(Long currentUserId, Long targetUserId) {
        User targetUser = getUserById(targetUserId);
        return buildUserSearchResponse(targetUser, currentUserId);
    }

    // --- Helpers ---

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Friendship getFriendshipById(Long id) {
        return friendshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found"));
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
                .avatarEffectId(user.getAvatarEffectId())
                .bannerEffectId(user.getBannerEffectId())
                .cardEffectId(user.getCardEffectId())
                .friendshipStatus(status)
                .friendshipId(friendshipId)
                .isSender(isSender)
                .build();
    }
}

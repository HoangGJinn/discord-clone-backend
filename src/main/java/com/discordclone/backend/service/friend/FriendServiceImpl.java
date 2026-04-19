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

    // â”€â”€â”€ TÃ¬m kiáº¿m user â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Override
    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(String keyword, Long currentUserId) {
        if (keyword == null || keyword.trim().length() < 2) {
            throw new IllegalArgumentException("Tá»« khÃ³a tÃ¬m kiáº¿m pháº£i cÃ³ Ã­t nháº¥t 2 kÃ½ tá»±");
        }

        String trimmed = keyword.trim();

        // DÃ¹ng JPQL query tá»« UserRepository (case-insensitive, hiá»‡u quáº£ hÆ¡n findAll)
        List<User> users = userRepository.searchByKeyword(trimmed).stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .collect(Collectors.toList());

        return users.stream()
                .map(u -> buildUserSearchResponse(u, currentUserId))
                .collect(Collectors.toList());
    }

    // â”€â”€â”€ Gá»­i lá»i má»i â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Override
    public FriendshipResponse sendFriendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("KhÃ´ng thá»ƒ gá»­i lá»i má»i cho chÃ­nh mÃ¬nh");
        }

        User sender = getUserById(senderId);
        User receiver = getUserById(receiverId);

        // Kiá»ƒm tra Ä‘Ã£ cÃ³ má»‘i quan há»‡ chÆ°a
        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(senderId, receiverId);
        if (existing.isPresent()) {
            FriendshipStatus status = existing.get().getStatus();
            switch (status) {
                case ACCEPTED:
                    throw new IllegalArgumentException("Hai ngÆ°á»i Ä‘Ã£ lÃ  báº¡n bÃ¨");
                case PENDING:
                    throw new IllegalArgumentException("Lá»i má»i káº¿t báº¡n Ä‘Ã£ Ä‘Æ°á»£c gá»­i, Ä‘ang chá» xÃ¡c nháº­n");
                case BLOCKED:
                    throw new IllegalArgumentException("KhÃ´ng thá»ƒ gá»­i lá»i má»i do bá»‹ cháº·n");
                case REJECTED:
                    // Cho phÃ©p gá»­i láº¡i sau khi bá»‹ tá»« chá»‘i â€” reset vá» PENDING
                    Friendship old = existing.get();
                    old.setStatus(FriendshipStatus.PENDING);
                    // Äáº·t ngÆ°á»i gá»­i hiá»‡n táº¡i lÃ m sender
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

    // â”€â”€â”€ Cháº¥p nháº­n lá»i má»i â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Override
    public FriendshipResponse acceptFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship f = getFriendshipById(friendshipId);

        // Chá»‰ ngÆ°á»i nháº­n má»›i Ä‘Æ°á»£c cháº¥p nháº­n
        if (!f.getReceiver().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Báº¡n khÃ´ng cÃ³ quyá»n cháº¥p nháº­n lá»i má»i nÃ y");
        }
        if (f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Lá»i má»i khÃ´ng á»Ÿ tráº¡ng thÃ¡i chá» xÃ¡c nháº­n");
        }

        f.setStatus(FriendshipStatus.ACCEPTED);
        return FriendshipResponse.from(friendshipRepository.save(f));
    }

    // â”€â”€â”€ Tá»« chá»‘i lá»i má»i â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Override
    public FriendshipResponse rejectFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship f = getFriendshipById(friendshipId);

        // Chá»‰ ngÆ°á»i nháº­n má»›i Ä‘Æ°á»£c tá»« chá»‘i
        if (!f.getReceiver().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Báº¡n khÃ´ng cÃ³ quyá»n tá»« chá»‘i lá»i má»i nÃ y");
        }
        if (f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Lá»i má»i khÃ´ng á»Ÿ tráº¡ng thÃ¡i chá» xÃ¡c nháº­n");
        }

        f.setStatus(FriendshipStatus.REJECTED);
        return FriendshipResponse.from(friendshipRepository.save(f));
    }

    // â”€â”€â”€ Há»§y lá»i má»i Ä‘Ã£ gá»­i â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Override
    public void cancelFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship f = getFriendshipById(friendshipId);

        if (!f.getSender().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Báº¡n khÃ´ng cÃ³ quyá»n há»§y lá»i má»i nÃ y");
        }
        if (f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Chá»‰ cÃ³ thá»ƒ há»§y lá»i má»i Ä‘ang chá» xÃ¡c nháº­n");
        }

        friendshipRepository.delete(f);
    }

    // â”€â”€â”€ XÃ³a báº¡n bÃ¨ â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Override
    public void unfriend(Long friendshipId, Long currentUserId) {
        Friendship f = getFriendshipById(friendshipId);

        boolean isParticipant = f.getSender().getId().equals(currentUserId)
                || f.getReceiver().getId().equals(currentUserId);

        if (!isParticipant) {
            throw new IllegalArgumentException("Báº¡n khÃ´ng cÃ³ quyá»n thá»±c hiá»‡n hÃ nh Ä‘á»™ng nÃ y");
        }
        if (f.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new IllegalArgumentException("Hai ngÆ°á»i khÃ´ng pháº£i báº¡n bÃ¨");
        }

        friendshipRepository.delete(f);
    }

    // â”€â”€â”€ Block user â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

    // â”€â”€â”€ Danh sÃ¡ch báº¡n bÃ¨ â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

    // â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User khÃ´ng tá»“n táº¡i: " + id));
    }

    private Friendship getFriendshipById(Long id) {
        return friendshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y má»‘i quan há»‡ báº¡n bÃ¨"));
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

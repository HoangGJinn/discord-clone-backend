package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.ChannelReadState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelReadStateRepository extends JpaRepository<ChannelReadState, Long> {

    Optional<ChannelReadState> findByChannelIdAndUserId(Long channelId, Long userId);

    List<ChannelReadState> findByUserIdAndChannelIdIn(Long userId, Collection<Long> channelIds);
}

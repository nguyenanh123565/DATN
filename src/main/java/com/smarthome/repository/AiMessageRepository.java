package com.smarthome.repository;

import com.smarthome.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    List<AiMessage> findByConversationSessionIdOrderByCreatedAtAsc(String sessionId);
    List<AiMessage> findAllByOrderByCreatedAtDesc();
}

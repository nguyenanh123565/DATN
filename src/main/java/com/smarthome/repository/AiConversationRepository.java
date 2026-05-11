package com.smarthome.repository;

import com.smarthome.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    Optional<AiConversation> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}

package com.catalis.core.banking.cards.authorization.core.mappers;

import com.catalis.core.banking.cards.authorization.interfaces.dtos.AuthorizationDecisionDTO;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.BalanceSnapshotDTO;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.LimitSnapshotDTO;
import com.catalis.core.banking.cards.authorization.models.entities.AuthorizationDecision;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * Mapper for converting between AuthorizationDecision entity and AuthorizationDecisionDTO.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = {ObjectMapper.class}
)
public abstract class AuthorizationDecisionMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Convert a DTO to an entity.
     *
     * @param dto The AuthorizationDecisionDTO to convert
     * @return The converted AuthorizationDecision entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "decisionPath", expression = "java(convertListToJson(dto.getDecisionPath()))")
    @Mapping(target = "networkResponseData", expression = "java(convertMapToJson(dto.getNetworkResponseData()))")
    @Mapping(target = "additionalData", expression = "java(convertMapToJson(dto.getAdditionalData()))")
    @Mapping(target = "challengeData", expression = "java(convertMapToJson(dto.getChallengeData()))")
    @Mapping(target = "dailyLimit", source = "limitsSnapshot.dailyLimit")
    @Mapping(target = "dailySpent", source = "limitsSnapshot.dailySpent")
    @Mapping(target = "dailyRemaining", source = "limitsSnapshot.dailyRemaining")
    @Mapping(target = "monthlyLimit", source = "limitsSnapshot.monthlyLimit")
    @Mapping(target = "monthlySpent", source = "limitsSnapshot.monthlySpent")
    @Mapping(target = "monthlyRemaining", source = "limitsSnapshot.monthlyRemaining")
    @Mapping(target = "accountId", source = "balanceSnapshot.accountId")
    @Mapping(target = "availableBalanceBefore", source = "balanceSnapshot.availableBalanceBefore")
    @Mapping(target = "availableBalanceAfter", source = "balanceSnapshot.availableBalanceAfter")
    public abstract AuthorizationDecision toEntity(AuthorizationDecisionDTO dto);

    /**
     * Convert an entity to a DTO.
     *
     * @param entity The AuthorizationDecision entity to convert
     * @return The converted AuthorizationDecisionDTO
     */
    @Mapping(target = "decisionPath", expression = "java(convertJsonToList(entity.getDecisionPath()))")
    @Mapping(target = "networkResponseData", expression = "java(convertJsonToMap(entity.getNetworkResponseData()))")
    @Mapping(target = "additionalData", expression = "java(convertJsonToMap(entity.getAdditionalData()))")
    @Mapping(target = "challengeData", expression = "java(convertJsonToMap(entity.getChallengeData()))")
    @Mapping(target = "limitsSnapshot", expression = "java(createLimitsSnapshot(entity))")
    @Mapping(target = "balanceSnapshot", expression = "java(createBalanceSnapshot(entity))")
    public abstract AuthorizationDecisionDTO toDto(AuthorizationDecision entity);

    /**
     * Convert a list of entities to a list of DTOs.
     *
     * @param entities The list of AuthorizationDecision entities to convert
     * @return The list of converted AuthorizationDecisionDTOs
     */
    public abstract List<AuthorizationDecisionDTO> toDtoList(List<AuthorizationDecision> entities);

    /**
     * Update an existing entity with values from a DTO.
     *
     * @param dto The AuthorizationDecisionDTO containing the new values
     * @param entity The AuthorizationDecision entity to update
     * @return The updated AuthorizationDecision entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "decisionPath", expression = "java(convertListToJson(dto.getDecisionPath()))")
    @Mapping(target = "networkResponseData", expression = "java(convertMapToJson(dto.getNetworkResponseData()))")
    @Mapping(target = "additionalData", expression = "java(convertMapToJson(dto.getAdditionalData()))")
    @Mapping(target = "challengeData", expression = "java(convertMapToJson(dto.getChallengeData()))")
    @Mapping(target = "dailyLimit", source = "limitsSnapshot.dailyLimit")
    @Mapping(target = "dailySpent", source = "limitsSnapshot.dailySpent")
    @Mapping(target = "dailyRemaining", source = "limitsSnapshot.dailyRemaining")
    @Mapping(target = "monthlyLimit", source = "limitsSnapshot.monthlyLimit")
    @Mapping(target = "monthlySpent", source = "limitsSnapshot.monthlySpent")
    @Mapping(target = "monthlyRemaining", source = "limitsSnapshot.monthlyRemaining")
    @Mapping(target = "accountId", source = "balanceSnapshot.accountId")
    @Mapping(target = "availableBalanceBefore", source = "balanceSnapshot.availableBalanceBefore")
    @Mapping(target = "availableBalanceAfter", source = "balanceSnapshot.availableBalanceAfter")
    public abstract AuthorizationDecision updateEntityFromDto(AuthorizationDecisionDTO dto, @MappingTarget AuthorizationDecision entity);

    /**
     * Create a LimitSnapshotDTO from an AuthorizationDecision entity.
     *
     * @param entity The AuthorizationDecision entity
     * @return The created LimitSnapshotDTO
     */
    protected LimitSnapshotDTO createLimitsSnapshot(AuthorizationDecision entity) {
        if (entity == null) {
            return null;
        }
        
        return LimitSnapshotDTO.builder()
                .dailyLimit(entity.getDailyLimit())
                .dailySpent(entity.getDailySpent())
                .dailyRemaining(entity.getDailyRemaining())
                .monthlyLimit(entity.getMonthlyLimit())
                .monthlySpent(entity.getMonthlySpent())
                .monthlyRemaining(entity.getMonthlyRemaining())
                .snapshotDate(entity.getTimestamp() != null ? entity.getTimestamp().toLocalDate() : null)
                .build();
    }

    /**
     * Create a BalanceSnapshotDTO from an AuthorizationDecision entity.
     *
     * @param entity The AuthorizationDecision entity
     * @return The created BalanceSnapshotDTO
     */
    protected BalanceSnapshotDTO createBalanceSnapshot(AuthorizationDecision entity) {
        if (entity == null) {
            return null;
        }
        
        return BalanceSnapshotDTO.builder()
                .accountId(entity.getAccountId())
                .currency(entity.getCurrency())
                .availableBalanceBefore(entity.getAvailableBalanceBefore())
                .availableBalanceAfter(entity.getAvailableBalanceAfter())
                .timestamp(entity.getTimestamp())
                .build();
    }

    /**
     * Convert a list to a JSON string.
     *
     * @param list The list to convert
     * @return The JSON string
     */
    protected String convertListToJson(List<?> list) {
        if (list == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting list to JSON", e);
        }
    }

    /**
     * Convert a map to a JSON string.
     *
     * @param map The map to convert
     * @return The JSON string
     */
    protected String convertMapToJson(Map<?, ?> map) {
        if (map == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting map to JSON", e);
        }
    }

    /**
     * Convert a JSON string to a list.
     *
     * @param json The JSON string to convert
     * @return The list
     */
    @SuppressWarnings("unchecked")
    protected List<String> convertJsonToList(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to list", e);
        }
    }

    /**
     * Convert a JSON string to a map.
     *
     * @param json The JSON string to convert
     * @return The map
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> convertJsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to map", e);
        }
    }
}

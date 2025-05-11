package com.catalis.core.banking.cards.authorization.core.mappers;

import com.catalis.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.catalis.core.banking.cards.authorization.models.entities.AuthorizationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper for converting between AuthorizationRequest entity and AuthorizationRequestDTO.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AuthorizationRequestMapper {

    /**
     * Convert a DTO to an entity.
     *
     * @param dto The AuthorizationRequestDTO to convert
     * @return The converted AuthorizationRequest entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "processed", constant = "false")
    @Mapping(target = "processedAt", ignore = true)
    AuthorizationRequest toEntity(AuthorizationRequestDTO dto);

    /**
     * Convert an entity to a DTO.
     *
     * @param entity The AuthorizationRequest entity to convert
     * @return The converted AuthorizationRequestDTO
     */
    AuthorizationRequestDTO toDto(AuthorizationRequest entity);

    /**
     * Convert a list of entities to a list of DTOs.
     *
     * @param entities The list of AuthorizationRequest entities to convert
     * @return The list of converted AuthorizationRequestDTOs
     */
    List<AuthorizationRequestDTO> toDtoList(List<AuthorizationRequest> entities);

    /**
     * Update an existing entity with values from a DTO.
     *
     * @param dto The AuthorizationRequestDTO containing the new values
     * @param entity The AuthorizationRequest entity to update
     * @return The updated AuthorizationRequest entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    AuthorizationRequest updateEntityFromDto(AuthorizationRequestDTO dto, @MappingTarget AuthorizationRequest entity);

    /**
     * Mark an authorization request as processed.
     *
     * @param entity The AuthorizationRequest entity to mark as processed
     * @param processedAt The timestamp when the request was processed
     * @return The updated AuthorizationRequest entity
     */
    default AuthorizationRequest markAsProcessed(AuthorizationRequest entity, LocalDateTime processedAt) {
        entity.setProcessed(true);
        entity.setProcessedAt(processedAt);
        return entity;
    }
}

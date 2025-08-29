package com.firefly.core.banking.cards.authorization.core.mappers;

import com.firefly.core.banking.cards.authorization.interfaces.dtos.AuthorizationHoldDTO;
import com.firefly.core.banking.cards.authorization.models.entities.AuthorizationHold;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper for converting between AuthorizationHold entity and AuthorizationHoldDTO.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AuthorizationHoldMapper {

    /**
     * Convert a DTO to an entity.
     *
     * @param dto The AuthorizationHoldDTO to convert
     * @return The converted AuthorizationHold entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    AuthorizationHold toEntity(AuthorizationHoldDTO dto);

    /**
     * Convert an entity to a DTO.
     *
     * @param entity The AuthorizationHold entity to convert
     * @return The converted AuthorizationHoldDTO
     */
    AuthorizationHoldDTO toDto(AuthorizationHold entity);

    /**
     * Convert a list of entities to a list of DTOs.
     *
     * @param entities The list of AuthorizationHold entities to convert
     * @return The list of converted AuthorizationHoldDTOs
     */
    List<AuthorizationHoldDTO> toDtoList(List<AuthorizationHold> entities);

    /**
     * Update an existing entity with values from a DTO.
     *
     * @param dto The AuthorizationHoldDTO containing the new values
     * @param entity The AuthorizationHold entity to update
     * @return The updated AuthorizationHold entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    AuthorizationHold updateEntityFromDto(AuthorizationHoldDTO dto, @MappingTarget AuthorizationHold entity);

    /**
     * Update the capture status of an authorization hold.
     *
     * @param entity The AuthorizationHold entity to update
     * @param captureStatus The new capture status
     * @param capturedAmount The captured amount
     * @param capturedAt The timestamp when the hold was captured
     * @return The updated AuthorizationHold entity
     */
    default AuthorizationHold updateCaptureStatus(
            AuthorizationHold entity, 
            String captureStatus, 
            java.math.BigDecimal capturedAmount, 
            LocalDateTime capturedAt) {
        entity.setCaptureStatus(captureStatus);
        entity.setCapturedAmount(capturedAmount);
        entity.setCapturedAt(capturedAt);
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}

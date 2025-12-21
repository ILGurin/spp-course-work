package by.cloud.api.mapper;

import by.cloud.individual.dto.IndividualDto;
import by.cloud.individual.dto.IndividualWriteDto;
import by.cloud.individual.dto.IndividualWriteResponseDto;
import org.mapstruct.Mapper;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR)
public interface PersonMapper {

    by.cloud.person.dto.IndividualWriteDto from(IndividualWriteDto dto);

    by.cloud.person.dto.IndividualDto from(IndividualDto dto);

    IndividualDto from(by.cloud.person.dto.IndividualDto dto);

    IndividualWriteResponseDto from(by.cloud.person.dto.IndividualWriteResponseDto dto);

}

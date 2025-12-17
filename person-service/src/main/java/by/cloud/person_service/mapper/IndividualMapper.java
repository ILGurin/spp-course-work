package by.cloud.person_service.mapper;

import by.cloud.person.dto.IndividualDto;
import by.cloud.person_service.entity.Individual;
import by.cloud.person_service.util.DateTimeUtil;
import by.cloud.person.dto.IndividualWriteDto;
import lombok.Setter;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import static org.springframework.util.CollectionUtils.isEmpty;

@Mapper(
        componentModel = SPRING,
        injectionStrategy = CONSTRUCTOR,
        uses = {
                UserMapper.class,
        }
)
@Setter(onMethod_ = @Autowired)
public abstract class IndividualMapper {

    protected DateTimeUtil dateTimeUtil;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "created", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "user", source = ".", qualifiedByName = "toUser")
    public abstract Individual to(IndividualWriteDto dto);

    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "email", source = "user.email")
    public abstract IndividualDto from(Individual individual);

    public List<IndividualDto> from(List<Individual> individuals) {
        return isEmpty(individuals)
                ? Collections.emptyList()
                : individuals.stream()
                .map(this::from)
                .collect(Collectors.toList());
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "user", expression = "java(userMapper.update(individual, dto))")
    public abstract void update(
            @MappingTarget
            Individual individual,
            IndividualWriteDto dto
    );
}

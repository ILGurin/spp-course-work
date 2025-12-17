package by.cloud.person_service.mapper;

import by.cloud.person.dto.IndividualWriteDto;
import by.cloud.person_service.entity.Individual;
import by.cloud.person_service.entity.User;
import by.cloud.person_service.util.DateTimeUtil;
import lombok.Setter;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        injectionStrategy = CONSTRUCTOR
)
@Setter(onMethod_ = @Autowired)
public abstract class UserMapper {

    protected DateTimeUtil dateTimeUtil;

    @Named("toUser")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "created", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    public abstract User to(IndividualWriteDto dto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    public abstract User update(
            @MappingTarget
            User user,
            IndividualWriteDto dto
    );

    public User update(Individual individual, IndividualWriteDto dto) {
        return update(individual.getUser(), dto);
    }
}

package by.cloud.file_service.mapper;

import by.cloud.directory.dto.DirectoryDto;
import by.cloud.directory.dto.DirectoryWriteDto;
import by.cloud.file_service.entity.Directory;
import by.cloud.file_service.util.DateTimeUtil;
import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        injectionStrategy = CONSTRUCTOR
)
@Setter(onMethod_ = @Autowired)
public abstract class DirectoryMapper {

    protected DateTimeUtil dateTimeUtil;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "created", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    public abstract Directory to(DirectoryWriteDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "created", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "name", constant = "/")
    @Mapping(target = "path", constant = "/")
    @Mapping(target = "parentId", ignore = true)
    public abstract Directory toBaseDirectory(UUID userId);

    public abstract DirectoryDto from(Directory directory);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    public abstract void update(@org.mapstruct.MappingTarget Directory directory, DirectoryWriteDto dto);
}

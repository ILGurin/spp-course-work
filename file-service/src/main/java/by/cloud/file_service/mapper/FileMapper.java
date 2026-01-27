package by.cloud.file_service.mapper;

import by.cloud.file.dto.FileDto;
import by.cloud.file_service.entity.File;
import by.cloud.file_service.util.DateTimeUtil;
import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        injectionStrategy = CONSTRUCTOR
)
@Setter(onMethod_ = @Autowired)
public abstract class FileMapper {

    protected DateTimeUtil dateTimeUtil;

    @Mapping(target = "directoryId", source = "directory.id")
    @Mapping(target = "downloadUrl", ignore = true)
    public abstract FileDto toDto(File file);
}

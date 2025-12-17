package by.cloud.person_service.service;

import by.cloud.person.dto.IndividualDto;
import by.cloud.person.dto.IndividualPageDto;
import by.cloud.person.dto.IndividualWriteDto;
import by.cloud.person.dto.IndividualWriteResponseDto;
import by.cloud.person_service.entity.Individual;
import by.cloud.person_service.exception.PersonException;
import by.cloud.person_service.mapper.IndividualMapper;
import by.cloud.person_service.repository.IndividualRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Setter
@Service
@RequiredArgsConstructor
public class IndividualService {
    private final IndividualMapper individualMapper;
    private final IndividualRepository individualRepository;

    @Transactional
    public IndividualWriteResponseDto register(IndividualWriteDto writeDto) {
        var individual = individualMapper.to(writeDto);
        individualRepository.save(individual);
        log.info("IN - register: individual: [{}] successfully registered", individual.getUser().getEmail());
        return new IndividualWriteResponseDto(individual.getId().toString());
    }

    public IndividualPageDto findByEmails(List<String> emails) {
        var individuals = individualRepository.findAllByEmails(emails);
        var from = individualMapper.from(individuals);
        var individualPageDto = new IndividualPageDto();
        individualPageDto.setItems(from);
        return individualPageDto;
    }

    public IndividualDto findById(UUID id) {
        var individual = individualRepository.findById(id)
                        .orElseThrow(() -> new PersonException("Individual not found by id=[%s]", id));
        log.info("IN - findById: individual with id = [{}] successfully found", id);
        return individualMapper.from(individual);
    }

    @Transactional
    public void softDelete(UUID id) {
        log.info("IN - softDelete: individual with id = [{}] successfully deleted", id);
        individualRepository.softDelete(id);
    }

    @Transactional
    public void hardDelete(UUID id) {
        var individual = individualRepository.findById(id)
                .orElseThrow(() -> new PersonException("Individual not found by id=[%s]", id));
        log.info("IN - hardDelete: individual with id = [{}] successfully deleted", id);
        individualRepository.delete(individual);
    }

    @Transactional
    public IndividualWriteResponseDto update(UUID id, IndividualWriteDto writeDto) {
        var individual = individualRepository.findById(id)
                .orElseThrow(() -> new PersonException("Individual not found by id=[%s]", id));
        individualMapper.update(individual, writeDto);
        individualRepository.save(individual);
        return new IndividualWriteResponseDto(individual.getId().toString());
    }
}

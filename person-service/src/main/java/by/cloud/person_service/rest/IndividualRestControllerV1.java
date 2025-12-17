package by.cloud.person_service.rest;

import by.cloud.person.api.PersonApi;
import by.cloud.person.dto.IndividualDto;
import by.cloud.person.dto.IndividualPageDto;
import by.cloud.person.dto.IndividualWriteDto;
import by.cloud.person.dto.IndividualWriteResponseDto;
import by.cloud.person_service.service.IndividualService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Email;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class IndividualRestControllerV1 implements PersonApi {
    private final IndividualService individualService;

    @Override
    public ResponseEntity<Void> compensateRegistration(UUID id) {
        individualService.hardDelete(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> delete(UUID id) {
        individualService.softDelete(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<IndividualPageDto> findAllByEmail(List<@Email String> email) {
        var individuals = individualService.findByEmails(email);
        return ResponseEntity.ok(individuals);
    }

    @Override
    public ResponseEntity<IndividualDto> findById(UUID id) {
        var individual = individualService.findById(id);
        return ResponseEntity.ok(individual);
    }

    @Override
    public ResponseEntity<IndividualWriteResponseDto> registration(IndividualWriteDto individualWriteDto) {
        var individual = individualService.register(individualWriteDto);
        return ResponseEntity.ok(individual);
    }

    @Override
    public ResponseEntity<IndividualWriteResponseDto> update(UUID id, IndividualWriteDto individualWriteDto) {
        return ResponseEntity.ok(individualService.update(id, individualWriteDto));
    }
}

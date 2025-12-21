package by.cloud.api.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import by.cloud.api.mapper.PersonMapper;
import by.cloud.individual.dto.IndividualWriteDto;
import by.cloud.individual.dto.IndividualWriteResponseDto;
import by.cloud.person.api.PersonApiClient;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonService {
    // Класс "оборачивает" обращения к person-service через feign клиент - PersonApiClient

    private final PersonApiClient personApiClient;
    private final PersonMapper personMapper;

    @WithSpan("personService.register") // Аннотация нужна для трассировки. Указывает span в данной цепочке вызовов
    public Mono<IndividualWriteResponseDto> register(IndividualWriteDto request) {
        return Mono.fromCallable(() -> personApiClient.registration(personMapper.from(request)))
                .mapNotNull(HttpEntity::getBody)
                .map(personMapper::from)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(t -> log.info("Person registered id = [{}]", t.getId()));
    }

    @WithSpan("personService.compensateRegistration")
    public Mono<Void> compensateRegistration(String id) {
        return Mono.fromRunnable(() -> personApiClient.compensateRegistration(UUID.fromString(id)))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}

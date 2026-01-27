package by.cloud.api;

import by.cloud.keycloak.api.AuthApiClient;
import by.cloud.person.api.PersonApiClient;
import by.cloud.file.api.FileApiClient;
import by.cloud.directory.api.DirectoryApiClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackageClasses = {AuthApiClient.class, PersonApiClient.class, FileApiClient.class, DirectoryApiClient.class})
@ConfigurationPropertiesScan
@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}

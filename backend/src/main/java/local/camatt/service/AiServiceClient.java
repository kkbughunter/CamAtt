package local.camatt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Service
public class AiServiceClient {
    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);
    private final RestTemplate client = new RestTemplate();
    private final String serviceUrl;
    public AiServiceClient(@Value("${camatt.ai-service-url}") String url) { serviceUrl = url; }
    public boolean register(Long employeeId, List<StorageService.StoredPhoto> photos) {
        try {
            var body = new LinkedMultiValueMap<String, Object>();
            body.add("employee_id", employeeId.toString());
            photos.forEach(photo -> body.add("photos", new FileSystemResource(photo.path())));
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            RegistrationResult result = client.postForObject(serviceUrl + "/faces/register", new HttpEntity<>(body, headers), RegistrationResult.class);
            return result != null && result.registered();
        } catch (Exception error) {
            log.warn("Face registration failed for employee {}: {}", employeeId, error.getMessage());
            return false;
        }
    }
    private record RegistrationResult(boolean registered, int samples, String message) {}
}

package com.tdarquier.tfg.request_service.services;

import com.tdarquier.tfg.request_service.clients.UserActivitiesClient;
import com.tdarquier.tfg.request_service.clients.UserClient;
import com.tdarquier.tfg.request_service.kafka.SGRProducer;
import com.tdarquier.tfg.request_service.messages.GenerationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class GenerationRequestServiceImpl implements GenerationRequestService {

    private final UserClient userClient;

    private final UserActivitiesClient userActivitiesClient;

    private final SGRProducer sgrProducer;

    public GenerationRequestServiceImpl(UserClient userClient, UserActivitiesClient userActivitiesClient, SGRProducer sgrProducer) {
        this.userClient = userClient;
        this.userActivitiesClient = userActivitiesClient;
        this.sgrProducer = sgrProducer;
    }


    @Override
    public boolean sendGenerationRequest(GenerationRequest generationRequest) {
        Long id = generationRequest.userId();
        if(userClient.existsById(id)) {
            userActivitiesClient.CodeGenerationActivity(id);
            sgrProducer.sendGenerationRequest(generationRequest);
            return true;
        }
        return false;
    }
}
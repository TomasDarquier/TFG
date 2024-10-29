package com.tdarquier.tfg.generation_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tdarquier.tfg.generation_service.authentication.RequestContext;
import com.tdarquier.tfg.generation_service.clients.InitClient;
import com.tdarquier.tfg.generation_service.kafka.generation.GenerationRequest;
import com.tdarquier.tfg.generation_service.services.RdfService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;

// services-generation-request consumer
@Service
@RequiredArgsConstructor
public class SGRConsumer {

    private final RequestContext requestContext;
    RdfService rdfService;
    InitClient initClient;

    @Autowired
    public SGRConsumer(RdfService rdfService, InitClient initClient, RequestContext requestContext) {
        this.rdfService = rdfService;
        this.initClient = initClient;
        this.requestContext = requestContext;
    }

    @KafkaListener(topics = "services-generation-request-topic")
    public void consumeGenerationRequest(ConsumerRecord<String, GenerationRequest> record) {

        //obtener JWT para validar peticiones con feign
        String jwt = null;
        for(Header header : record.headers()) {
            if(header.key().equals("Authorization")) {
                jwt = new String(header.value());
                break;
            }
        }

        requestContext.initialize(jwt);

        try {
            //transformar JSON a rdf
            String projectRDF = String.valueOf(rdfService.toRdf(record.value()));

            //Enviar RDF a  init-service
            List<String> poms = initClient.getPoms(projectRDF);

            // -- Enviar notificacion
            //Enviar poms a code-service
            // codeClient.generateCode(projectRDF, projectPoms);
            // -- Enviar notificacion
            //Recibir confirmacion de finalizacion
            // -- Enviar notificacion
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }finally {
            requestContext.clear();
        }


    }
}

package com.tdarquier.tfg.code_service.services;

import com.tdarquier.tfg.code_service.entities.ComponentData;
import com.tdarquier.tfg.code_service.entities.MinioFile;
import com.tdarquier.tfg.code_service.enums.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DynamicCodeGenerationService {

    private final VelocityEngine velocityEngine;
    private final MinioService minioService;

    public DynamicCodeGenerationService(MinioService minioService) {
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loader", "classpath");
        velocityEngine.setProperty("classpath.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        this.minioService = minioService;
    }

    public void generateServiceCode(ComponentData componentData,String bucket){
        List<MinioFile> files = generateJavaCode(componentData, bucket);

        files.addAll(generateProperties(componentData, bucket));

        // si no es un util service genera dockerfile
        if(!isUtilService(componentData.getTemplate())){
            files.add(generateDockerFile(componentData,bucket));
        }

        files.forEach(minioService::saveObject);
    }

    private boolean isConfigService(ComponentData componentData) {
        return componentData.getTemplate()
                .toString()
                .toLowerCase()
                .startsWith("configuration_service");
    }

    private boolean isUtilService(Template template) {
        return template.toString().toLowerCase().startsWith("configuration_service") ||
                template.toString().toLowerCase().startsWith("gateway_service") ||
                template.toString().toLowerCase().startsWith("discovery_service");
    }

    private MinioFile generateDockerFile(ComponentData componentData, String bucket) {
        String templateFolder = componentData.getTemplate().toString().toLowerCase() + "_docker/";
        String fileContent = generateFile(templateFolder + "docker-compose.vm", componentData);
        return new MinioFile(
                componentData.getPaths().get("service") + "docker-compose.yml",
                bucket,
                fileContent
        );
    }

    private List<MinioFile> generateJavaCode(ComponentData componentData, String bucket) {
        List<MinioFile> generatedFiles = new ArrayList<>();
        String templateFolder = componentData.getTemplate().toString().toLowerCase();
        String filesList;
        try {
            filesList = readFileFromResources(templateFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if(filesList == null || filesList.isEmpty()){
            return new ArrayList<>();
        }
        //lista de todos los archivos pertenecientes a la plantilla del servicio
        List<String> filesToGenerate = Arrays.stream(filesList.split("\n")).toList();

        //generar cada archivo
        filesToGenerate.forEach(file -> {
            String fileContent = generateFile(templateFolder + "/" + file, componentData);
            //ej de nombre de template service-UserServiceImp.vm (-) representa /
            String formatedPath = file.replaceAll("-","/"); // service/UserServiceImp.vm
            String fileNameInJavaType;
            // si es la App class, se formatea para que tanto el file como la class se llame como se nombro al servicio
            if(file.equals("ApplicationClass.vm")){
                fileNameInJavaType = createApplicationName(componentData.getName()) + ".java";
            }else {
                fileNameInJavaType = formatedPath.substring(0, formatedPath.indexOf('.')) + ".java"; // service/UserService.java
            }
            generatedFiles.add(new MinioFile(
                    componentData.getPaths().get("code") + fileNameInJavaType,
                    bucket,
                    fileContent
            ));
        });

        //TODO testFile
        generatedFiles.add(generateTestFile(componentData,bucket));

        return generatedFiles;
    }

    private MinioFile generateTestFile(ComponentData componentData, String bucket) {
        return null;
    }

    private String generateFile(String templatePath, ComponentData componentData) {
        String fullPath = "/dynamic-files/" + templatePath;
        VelocityContext context= new VelocityContext();
        context.put("name",componentData.getName().replace(" ", "-"));
        context.put("connections", componentData.getConnections());
        context.put("package",componentData.getPaths().get("package"));
        context.put("apiPath",componentData.getApiPath());
        context.put("port", componentData.getPort());
        context.put("isConfSvEnabled",componentData.getIsConfigServerEnabled());
        context.put("applicationClassName",createApplicationName(componentData.getName()));
        if(!isUtilService(componentData.getTemplate())){
            context.put("persistence",componentData.getPersistenceType().toString());
        }

        var template = velocityEngine.getTemplate(fullPath);

        // se transforma el contenido generado de la plantilla en string
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    private String createApplicationName(String appName) {
        String formatedAppName = appName.replace(" ", "-");
        String[] words = formatedAppName.split("-");

        StringBuilder result = new StringBuilder(capitalFirstChar(words[0]));

        for (int i = 1; i < words.length; i++) {
            result.append(capitalFirstChar(words[i]));
        }

        return result.toString();
    }

    private static String capitalFirstChar(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }

    private List<MinioFile> generateProperties(ComponentData componentData, String bucket) {
        List<MinioFile> configFiles = new ArrayList<>();
        String templateFolder = componentData.getTemplate().toString().toLowerCase() + "_properties";

        if(componentData.getIsConfigServerEnabled() && !isConfigService(componentData)){
            String externalPropPath = "config-server/src/main/resources/configurations/"
                    + componentData.getName().replace(" ","-")
                    + ".properties";
            configFiles.add(new MinioFile(
                    externalPropPath,
                    bucket,
                    generateFile(templateFolder + "/application-properties-for-conf-sv.vm", componentData)
            ));
        }
        configFiles.add(new MinioFile(
                componentData.getPaths().get("resources") + "application.properties",
                bucket,
                generateFile(templateFolder + "/application-properties.vm", componentData)
        ));

        return configFiles;
    }

    //codigo duplicado, deberia crear un recurso global
    private String readFileFromResources(String fileName) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/dynamic-files/" + fileName);
        if (inputStream == null) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            return content.toString().trim(); // Retorna el contenido sin el último salto de línea
        }
    }
}

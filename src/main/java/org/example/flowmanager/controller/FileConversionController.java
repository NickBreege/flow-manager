package org.example.flowmanager.controller;

import lombok.RequiredArgsConstructor;
import org.example.flowmanager.model.dto.CreateConversionResponse;
import org.example.flowmanager.model.entity.FileConversion;
import org.example.flowmanager.service.FileConversionService;
import org.example.flowmanager.service.MinioService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;

import static org.springframework.http.HttpStatus.ACCEPTED;

@RestController
@RequestMapping("/api/v1/conversions")
@RequiredArgsConstructor
public class FileConversionController {
    private final FileConversionService fileConversionService;
    private final MinioService minioService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateConversionResponse> createConversion(@RequestPart("file") MultipartFile file) {
        FileConversion conversion = fileConversionService.createConversion(file);
        CreateConversionResponse response = new CreateConversionResponse(conversion.getId(), conversion.getStatus());

        return ResponseEntity.status(ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreateConversionResponse> getConversionStatus(@PathVariable Long id) {
        FileConversion conversion = fileConversionService.getConversion(id);
        CreateConversionResponse response = new CreateConversionResponse(conversion.getId(), conversion.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<StreamingResponseBody> downloadConversion(@PathVariable Long id) {
        String filePath = fileConversionService.getConvertedFilePath(id);

        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream inputStream = minioService.download(filePath)) {
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("conversion-" + id + ".pdf")
                                .build()
                                .toString())
                .body(responseBody);

    }
}

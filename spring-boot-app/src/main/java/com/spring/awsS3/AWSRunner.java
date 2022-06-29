package com.spring.awsS3;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.spring.awsS3.AWSConfig.ImageResponse;

@RestController
public class AWSRunner {

    @Autowired
    private AWSConfig awsConfig;

    @PostMapping("/upload")
    public ResponseEntity<String> run(@RequestBody MultipartFile file) {
        String uploadImage = awsConfig.uploadImage(file);
        return ResponseEntity.ok(uploadImage);
    }

    @GetMapping("/download")
    public ResponseEntity<ByteArrayResource> downloadFile(@RequestParam("name") String fileName) {
        byte[] image = awsConfig.getImageFromS3(fileName);
        ByteArrayResource response = new ByteArrayResource(image);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(response);
    }

    @GetMapping("/image/**")
    public void showImage(HttpServletRequest request, HttpServletResponse response) {
        try {
            String fileName = request.getRequestURI().split(request.getContextPath() + "/image/")[1];
            ImageResponse file = awsConfig.getFileFromS3Bucket(fileName);
            response.setContentType(file.getContentType());
            FileCopyUtils.copy(file.getInputStream(), response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

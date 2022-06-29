package com.spring.awsS3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import java.io.ByteArrayInputStream;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class AWSConfig {

    private AmazonS3 amazonS3;

    @Value("${aws.accessKey}")
    private String accessKey;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.bucketName}")
    private String bucketName;

    private String domain = "http://localhost:8080/image/";

    @PostConstruct
    public void initializeAws() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        amazonS3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.US_EAST_2)
                .build();
    }

    public String uploadImage(MultipartFile multipartFile) {
        File file = convertMultipartToFile(multipartFile);
        String fileName = System.currentTimeMillis() + "_" + file.getName();
        amazonS3.putObject(new PutObjectRequest(bucketName, fileName, file));
        file.delete();
        return "savedFile : " + domain + fileName;
    }

    public ImageResponse getFileFromS3Bucket(String fileName) {
        try {
            S3Object object = amazonS3.getObject(bucketName, fileName);
            System.out.println("image : " + object.getKey());
            try (S3ObjectInputStream stream = object.getObjectContent()) {
                ByteArrayOutputStream temp = new ByteArrayOutputStream();
                IOUtils.copy(stream, temp);
                InputStream is = new ByteArrayInputStream(temp.toByteArray());
                return new ImageResponse(is, object.getObjectMetadata().getContentType(),
                        object.getObjectMetadata().getContentLength());
            }
        } catch (SdkClientException | IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public byte[] getImageFromS3(String fileName) {
        try {
            S3Object object = amazonS3.getObject(bucketName, fileName);
            System.out.println("image : " + object.getKey());
            return object.getObjectContent().readAllBytes();
        } catch (SdkClientException | IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public String deleteImageFromS3(String fileName) {
        amazonS3.deleteObject(bucketName, fileName);
        return "deleted file : " + fileName;
    }

    private File convertMultipartToFile(MultipartFile multipartFile) {
        File file = new File(multipartFile.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    public class ImageResponse {

        private InputStream inputStream;
        private String contentType;
        private Long contentSize;

        public ImageResponse(InputStream inputStream, String contentType, Long contentSize) {
            super();
            this.inputStream = inputStream;
            this.contentType = contentType;
            this.contentSize = contentSize;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public void setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public Long getContentSize() {
            return contentSize;
        }

        public void setContentSize(Long contentSize) {
            this.contentSize = contentSize;
        }
    }
}

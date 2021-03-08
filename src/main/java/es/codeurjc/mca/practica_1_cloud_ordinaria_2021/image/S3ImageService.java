package es.codeurjc.mca.practica_1_cloud_ordinaria_2021.image;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service("storageService")
@Profile("production")
public class S3ImageService implements ImageService {

    @Value("${amazon.s3.bucket-name}")
    private String BUCKET_NAME;

    @Value("${amazon.s3.endpoint}")
    private String ENDPOINT;

    private final AmazonS3 s3;

    public S3ImageService(@Value("${amazon.s3.region}") String region) {
        this.s3 = AmazonS3ClientBuilder
                .standard()
                .withRegion(region)
                .build();
    }

    @Override
    public String createImage(MultipartFile multiPartFile) {
        createBucketIfNotExists();

        String fileName = "image_" + UUID.randomUUID() + "_" +multiPartFile.getOriginalFilename();
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + fileName);
        try {
            multiPartFile.transferTo(file);
        } catch (IOException e) {
            throw new IllegalStateException("File upload failed");
        }

        PutObjectRequest por = new PutObjectRequest(BUCKET_NAME, fileName, file);
        por.setCannedAcl(CannedAccessControlList.PublicRead);
        s3.putObject(por);

        return ENDPOINT + fileName;
    }

    private void createBucketIfNotExists() {
        if (!s3.doesBucketExistV2(BUCKET_NAME)) s3.createBucket(BUCKET_NAME);
    }

    @Override
    public void deleteImage(String image) {
        String[] imageUrl = image.split("/");
        s3.deleteObject(BUCKET_NAME, imageUrl[imageUrl.length - 1]);
    }

}

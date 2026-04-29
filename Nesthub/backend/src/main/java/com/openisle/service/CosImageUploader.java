package com.openisle.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

/**
 * ImageUploader implementation using Tencent Cloud COS.
 */
@Service
public class CosImageUploader extends ImageUploader {

  private final COSClient cosClient;
  private final String bucketName;
  private final String baseUrl;
  private static final String UPLOAD_DIR = "dynamic_assert/";
  private static final Logger logger = LoggerFactory.getLogger(CosImageUploader.class);
  private final ExecutorService executor = Executors.newFixedThreadPool(
    2,
    new CustomizableThreadFactory("cos-upload-")
  );

  @org.springframework.beans.factory.annotation.Autowired
  public CosImageUploader(
    com.openisle.repository.ImageRepository imageRepository,
    @Value("${cos.secret-id:}") String secretId,
    @Value("${cos.secret-key:}") String secretKey,
    @Value("${cos.region:ap-guangzhou}") String region,
    @Value("${cos.bucket-name:}") String bucketName,
    @Value("${cos.base-url:https://example.com}") String baseUrl
  ) {
    super(imageRepository, baseUrl);
    COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
    ClientConfig config = new ClientConfig(new Region(region));
    this.cosClient = new COSClient(cred, config);
    this.bucketName = bucketName;
    this.baseUrl = baseUrl;
    logger.debug("COS client initialized for region {} with bucket {}", region, bucketName);
  }

  // for tests
  CosImageUploader(
    COSClient cosClient,
    com.openisle.repository.ImageRepository imageRepository,
    String bucketName,
    String baseUrl
  ) {
    super(imageRepository, baseUrl);
    this.cosClient = cosClient;
    this.bucketName = bucketName;
    this.baseUrl = baseUrl;
    logger.debug("COS client provided directly with bucket {}", bucketName);
  }

  @Override
  protected CompletableFuture<String> doUpload(byte[] data, String filename) {
    return CompletableFuture.supplyAsync(
      () -> {
        logger.debug("Uploading {} bytes as {}", data.length, filename);
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot != -1) {
          ext = filename.substring(dot);
        }
        String randomName = UUID.randomUUID().toString().replace("-", "") + ext;
        String objectKey = UPLOAD_DIR + randomName;
        logger.debug("Generated object key {}", objectKey);

        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(data.length);
        PutObjectRequest req = new PutObjectRequest(
          bucketName,
          objectKey,
          new ByteArrayInputStream(data),
          meta
        );
        logger.debug("Sending PutObject request to bucket {}", bucketName);
        cosClient.putObject(req);
        String url = baseUrl + "/" + objectKey;
        logger.debug("Upload successful, accessible at {}", url);
        return url;
      },
      executor
    );
  }

  @Override
  protected void deleteFromStore(String key) {
    try {
      cosClient.deleteObject(bucketName, key);
    } catch (Exception e) {
      logger.warn("Failed to delete image {} from COS", key, e);
    }
  }

  @Override
  public java.util.Map<String, String> presignUpload(String filename) {
    String ext = "";
    int dot = filename.lastIndexOf('.');
    if (dot != -1) {
      ext = filename.substring(dot);
    }
    String randomName = java.util.UUID.randomUUID().toString().replace("-", "") + ext;
    String objectKey = UPLOAD_DIR + randomName;
    java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + 15 * 60 * 1000L);
    GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(
      bucketName,
      objectKey,
      HttpMethodName.PUT
    );
    req.setExpiration(expiration);
    java.net.URL url = cosClient.generatePresignedUrl(req);
    String fileUrl = baseUrl + "/" + objectKey;
    return java.util.Map.of("uploadUrl", url.toString(), "fileUrl", fileUrl, "key", objectKey);
  }

  /**
   * 本地上传（当 COS 未配置时使用）
   */
  public String uploadLocal(MultipartFile file) {
    try {
      String originalFilename = file.getOriginalFilename();
      String extension = "";
      if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
      }
      String filename = UUID.randomUUID().toString().replace("-", "") + extension;

      // 修改：使用相对于工作目录的路径，打包前后一致
      Path uploadDir = Path.of("uploads").toAbsolutePath().normalize();
      logger.debug("uploadLocal - 上传目录: {}", uploadDir.toString());
      if (!Files.exists(uploadDir)) {
        Files.createDirectories(uploadDir);
        logger.debug("uploadLocal - 创建上传目录: {}", uploadDir.toString());
      }

      Path dest = uploadDir.resolve(filename);
      file.transferTo(dest.toFile());

      logger.debug("本地文件上传成功: {} -> {}", filename, dest.toString());
      return "/uploads/" + filename;
    } catch (Exception e) {
      logger.error("本地文件上传失败", e);
      throw new RuntimeException("文件上传失败", e);
    }
  }

  /**
   * 从字节数组本地上传
   */
  public String uploadLocalFromBytes(byte[] data, String filename) {
    try {
      String extension = "";
      if (filename != null && filename.contains(".")) {
        extension = filename.substring(filename.lastIndexOf("."));
      }
      String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

      // 修改：使用相对于工作目录的路径，打包前后一致
      Path uploadDir = Path.of("uploads").toAbsolutePath().normalize();
      logger.debug("uploadLocalFromBytes - 上传目录: {}", uploadDir.toString());
      if (!Files.exists(uploadDir)) {
        Files.createDirectories(uploadDir);
        logger.debug("uploadLocalFromBytes - 创建上传目录: {}", uploadDir.toString());
      }

      Path dest = uploadDir.resolve(newFilename);
      Files.write(dest, data);

      logger.debug("本地字节上传成功: {} -> {}", newFilename, dest.toString());
      return "/uploads/" + newFilename;
    } catch (Exception e) {
      logger.error("本地文件上传失败", e);
      throw new RuntimeException("文件上传失败", e);
    }
  }
}
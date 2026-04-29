package com.openisle.controller;

import com.openisle.service.CosImageUploader;
import com.openisle.service.ImageUploader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

  private final ImageUploader imageUploader;
  private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

  @Value("${app.upload.check-type:true}")
  private boolean checkImageType;

  @Value("${app.upload.max-size:5242880}")
  private long maxUploadSize;

  @Value("${cos.bucket-name:}")
  private String bucketName;

  @PostMapping
  @Operation(summary = "Upload file", description = "Upload image file")
  @ApiResponse(
    responseCode = "200",
    description = "Upload result",
    content = @Content(schema = @Schema(implementation = java.util.Map.class))
  )
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
    if (
      checkImageType &&
      (file.getContentType() == null || !file.getContentType().startsWith("image/"))
    ) {
      return ResponseEntity.badRequest().body(Map.of("code", 1, "msg", "File is not an image"));
    }
    if (file.getSize() > maxUploadSize) {
      return ResponseEntity.badRequest().body(Map.of("code", 2, "msg", "File too large"));
    }
    String url;
    try {
      // 判断使用 COS 还是本地存储
      if (bucketName != null && !bucketName.isEmpty()) {
        url = imageUploader.upload(file.getBytes(), file.getOriginalFilename()).join();
      } else {
        url = ((CosImageUploader) imageUploader).uploadLocal(file);
      }
      logger.debug("上传成功: {}", url);
    } catch (IOException e) {
      logger.error("上传失败", e);
      return ResponseEntity.internalServerError().body(Map.of("code", 3, "msg", "Upload failed"));
    }
    return ResponseEntity.ok(Map.of("code", 0, "msg", "ok", "data", Map.of("url", url)));
  }

  @PostMapping("/url")
  @Operation(summary = "Upload from URL", description = "Upload image from remote URL")
  @ApiResponse(
    responseCode = "200",
    description = "Upload result",
    content = @Content(schema = @Schema(implementation = java.util.Map.class))
  )
  public ResponseEntity<?> uploadUrl(@RequestBody Map<String, String> body) {
    String link = body.get("url");
    if (link == null || link.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("code", 1, "msg", "Missing url"));
    }
    try {
      URL u = URI.create(link).toURL();
      byte[] data = u.openStream().readAllBytes();
      if (data.length > maxUploadSize) {
        return ResponseEntity.badRequest().body(Map.of("code", 2, "msg", "File too large"));
      }
      String filename = link.substring(link.lastIndexOf('/') + 1);
      String contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(data));
      if (checkImageType && (contentType == null || !contentType.startsWith("image/"))) {
        return ResponseEntity.badRequest().body(Map.of("code", 1, "msg", "File is not an image"));
      }
      String url;
      if (bucketName != null && !bucketName.isEmpty()) {
        url = imageUploader.upload(data, filename).join();
      } else {
        // 本地存储 URL 上传
        url = ((CosImageUploader) imageUploader).uploadLocalFromBytes(data, filename);
      }
      return ResponseEntity.ok(Map.of("code", 0, "msg", "ok", "data", Map.of("url", url)));
    } catch (Exception e) {
      logger.error("URL上传失败", e);
      return ResponseEntity.internalServerError().body(Map.of("code", 3, "msg", "Upload failed"));
    }
  }

  @GetMapping("/presign")
  @Operation(summary = "Presign upload", description = "Get presigned upload URL")
  @ApiResponse(
    responseCode = "200",
    description = "Presigned URL",
    content = @Content(schema = @Schema(implementation = java.util.Map.class))
  )
  public java.util.Map<String, String> presign(@RequestParam("filename") String filename) {
    // 本地存储不支持 presign，返回空
    if (bucketName == null || bucketName.isEmpty()) {
      return Map.of("error", "本地存储不支持预签名上传");
    }
    return imageUploader.presignUpload(filename);
  }
}
package com.library.system.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    String upload(MultipartFile file, String directory);

    String getUrl(String objectKey);

    void delete(String objectKey);
}

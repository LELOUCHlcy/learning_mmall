package com.mmall.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Created by lcy on 2017/12/24.
 */
public interface IFileService {

    String upload(MultipartFile multipartFile, String path);
}

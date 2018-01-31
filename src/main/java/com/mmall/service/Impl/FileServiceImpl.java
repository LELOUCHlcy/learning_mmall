package com.mmall.service.Impl;

import com.google.common.collect.Lists;
import com.mmall.service.IFileService;
import com.mmall.util.FTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by lcy on 2017/12/24.
 */
@Service("iFileService")
public class FileServiceImpl implements IFileService {

    private Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);
    @Override
    public String upload(MultipartFile multipartFile, String path) {
        String fileName = multipartFile.getOriginalFilename();
        String fileExtensionName = fileName.substring(fileName.lastIndexOf(".") + 1);
        String uploadFileName = UUID.randomUUID().toString() + "." + fileExtensionName;
        logger.info("开始上传文件，文件的原名称：{},文件的扩展后缀名:{},上传的文件名{}", fileName, fileExtensionName, uploadFileName);

        File fileDir = new File(path);

        if (!fileDir.exists()) {
            fileDir.setWritable(true);
            fileDir.mkdirs();
        }
        File uploadFile = new File(path, uploadFileName);
        try {
            //文件上传成功
            multipartFile.transferTo(uploadFile);
            //文件上传到服务器
            FTPUtil.uploadFile(Lists.newArrayList(uploadFile));
            //删除upload下的文件
            uploadFile.delete();
        } catch (IOException e) {
            logger.error("上传文件异常", e);
            return null;
        }

        return uploadFileName;
    }
}

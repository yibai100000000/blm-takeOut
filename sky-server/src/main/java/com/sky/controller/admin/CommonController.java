package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 文件相关通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {


    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 文件上传
     * @param file
     * @return
     */

    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file){
        log.info("正在上传:{}",file);

        try {
            //原始文件名
            String originalFilename = file.getOriginalFilename();
            //获取后缀
            String extension=originalFilename.substring(originalFilename.indexOf("."));
            //构造新文件名称
            String filePath= UUID.randomUUID().toString()+extension;


            String upload = aliOssUtil.upload(file.getBytes(), filePath);

            return Result.success(upload);
        } catch (Exception e) {

            e.printStackTrace();
            log.error("文件上传失败,{}",e);

        }
        return Result.error(MessageConstant.UPLOAD_FAILED);

    }
}

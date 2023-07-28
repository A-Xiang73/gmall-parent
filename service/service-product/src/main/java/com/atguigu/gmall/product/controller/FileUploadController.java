package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.minio.*;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author 24657
 * @apiNote
 * @date 2023/7/28 16:43
 */
@RestController
@RequestMapping("admin/product")
public class FileUploadController {
    //  获取文件上传对应的地址
    //使用minio的服务的url 端口1
    @Value("${minio.endpointUrl}")
    public String endpointUrl;

    @Value("${minio.accessKey}")
    public String accessKey;

    @Value("${minio.secreKey}")
    public String secreKey;

    @Value("${minio.bucketName}")
    public String bucketName;


    /*
    * 文件控制上传器
    * */
    @PostMapping("fileUpload")
    public Result fileUpload(MultipartFile file){

        try {
            String url="";
            // Create a minioClient with the MinIO server playground, its access key and secret key.
            //accesskey和cecretkey创建一个minioclient对象
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(endpointUrl)
                            .credentials(accessKey, secreKey)
                            .build();

            // Make 'asiatrip' bucket if not exist. 检查桶是否已经存在了
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                // Make a new bucket called 'asiatrip'. 不存在则创建
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            } else {
                System.out.println("Bucket"+bucketName+" already exists.");
            }

            // Upload '/home/user/Photos/asiaphotos.zip' as object name 'asiaphotos-2015.zip' to bucket
            // 'asiatrip'.
            //定义一个文件的名称：文件上传名称不能重复
            String fileName= System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 3);
            // Upload known sized input stream. 商品详情上传的时候会有异步操作
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            url = endpointUrl+"/"+bucketName+"/"+fileName;
            System.out.println("URL:"+url);
            return Result.ok(url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

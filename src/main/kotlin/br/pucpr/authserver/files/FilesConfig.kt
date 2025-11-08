package br.pucpr.authserver.files

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class FilesConfig {

    @Bean
    fun amazonS3Client(
        @Value("\${aws.credentials.access-key:}") accessKey: String,
        @Value("\${aws.credentials.secret-key:}") secretKey: String,
        @Value("\${aws.region}") awsRegion: String
    ): AmazonS3 {
        val credentialsProvider: AWSCredentialsProvider = if (accessKey.isNotEmpty() && secretKey.isNotEmpty()) {
            val credentials = BasicAWSCredentials(accessKey, secretKey)
            AWSStaticCredentialsProvider(credentials)
        } else {
            DefaultAWSCredentialsProviderChain()
        }

        return AmazonS3ClientBuilder.standard()
            .withRegion(Regions.fromName(awsRegion))
            .withCredentials(credentialsProvider)
            .build()
    }

    @Bean
    fun transferManager(s3Client: AmazonS3): TransferManager {
        return TransferManagerBuilder.standard()
            .withS3Client(s3Client)
            .build()
    }

    @Bean("fileStorage")
    @Profile("!fs")
    fun s3Storage(
        @Value("\${aws.credentials.access-key:}") accessKey: String,
        @Value("\${aws.credentials.secret-key:}") secretKey: String,
        @Value("\${aws.region}") awsRegion: String,
        @Value("\${storage.s3.bucket}") bucketName: String,
        @Value("\${storage.s3.base-url}") baseUrl: String,
        transferManager: TransferManager
    ): FileStorage {
        return S3Storage(
            accessKey, secretKey, awsRegion, bucketName, baseUrl, transferManager
        )
    }

    @Bean("fileStorage")
    @Profile("fs")
    fun localStorage() = FileSystemStorage()
}
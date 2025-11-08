package br.pucpr.authserver.files

import br.pucpr.authserver.files.FileSystemStorage.Companion.log
import br.pucpr.authserver.users.User
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class S3Storage(

    @Value("\${aws.credentials.access-key:}") private val accessKey: String,
    @Value("\${aws.credentials.secret-key:}") private val secretKey: String,
    @Value("\${aws.region}") private val awsRegion: String,
    @Value("\${storage.s3.bucket}") private val bucketName: String,
    @Value("\${storage.s3.base-url}") private val baseUrl: String,
    private val transferManager: TransferManager
) : FileStorage {

    private val s3: AmazonS3

    init {
        val credentialsProvider: AWSCredentialsProvider = if (accessKey.isNotEmpty() && secretKey.isNotEmpty()) {
            val credentials = BasicAWSCredentials(accessKey, secretKey)
            AWSStaticCredentialsProvider(credentials)
        } else {
            DefaultAWSCredentialsProviderChain()
        }

        this.s3 = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.fromName(awsRegion))
            .withCredentials(credentialsProvider)
            .build()
    }

    override fun save(
        user: User,
        path: String,
        file: MultipartFile
    ): String {
        val contentType = file.contentType ?: "application/octet-stream"

        val meta = ObjectMetadata()
        meta.contentType = contentType
        meta.contentLength = file.size
        meta.userMetadata["userId"] = "${user.id}"
        meta.userMetadata["originalFileName"] = file.originalFilename

        try {
            val upload = transferManager.upload(
                bucketName,
                path,
                file.inputStream,
                meta
            )
            upload.waitForCompletion()


            s3.setObjectAcl(bucketName, path, CannedAccessControlList.PublicRead)

            return path
        } catch (e: Exception) {
            throw RuntimeException("Falha ao salvar o arquivo '$path' no S3: ${e.message}", e)
        }
    }

    override fun load(path: String): Resource = InputStreamResource(
        s3
            .getObject(bucketName, path.replace("-S-", "/")) // Carrega o conteúdo do S3
            .objectContent
    )

    override fun remove(path: String) {
        try {
            log.info("aqui oh")
            s3.deleteObject(bucketName, path)
        } catch (e: com.amazonaws.services.s3.model.AmazonS3Exception) {
            log.error("ERRO FATAL S3 DELETE: Status Code: ${e.statusCode}, Error Code: ${e.errorCode}, Message: ${e.errorMessage}")
            throw RuntimeException("Falha na exclusão do S3: Permissão Negada ou Objeto não encontrado.", e)
        } catch (e: Exception) {
            log.error("ERRO INESPERADO ao deletar arquivo: ${e.message}", e)
            throw RuntimeException("Erro inesperado ao deletar no S3.", e)
        }
    }

    override fun urlFor(name: String): String =
        "${baseUrl}/${name}"
}

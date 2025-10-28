package br.pucpr.authserver.files

import br.pucpr.authserver.users.User
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile

class S3Storage : FileStorage {
    private val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
        .withRegion(Regions.US_EAST_1)
        .withCredentials(EnvironmentVariableCredentialsProvider())
        .build()

    override fun save(
        user: User,
        path: String,
        file: MultipartFile
    ): String {
        val contentType = file.contentType!!

        val transferManager = TransferManagerBuilder.standard()
            .withS3Client(s3)
            .build()

        val meta = ObjectMetadata()
        meta.contentType = contentType
        meta.contentLength = file.size
        meta.userMetadata["userId"] = "${user.id}"
        meta.userMetadata["originalFileName"] = file.name

        transferManager.upload(
            PUBLIC,
            path,
            file.inputStream,
            meta
        )
        return path
    }

    override fun load(path: String): Resource = InputStreamResource(
        s3
            .getObject(PUBLIC, path.replace("-S-", "/"))
            .objectContent
    )

    override fun urlFor(name: String): String =
        "https://$DISTRIBUTION.cloudfront.net/${name}"

    companion object {
        private const val PUBLIC = "vinigodoy-authserver-public"
        private const val DISTRIBUTION = "d2m8kv68a4r9i3"
    }
}

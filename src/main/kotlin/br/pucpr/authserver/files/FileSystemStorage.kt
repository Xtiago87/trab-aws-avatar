package br.pucpr.authserver.files

import br.pucpr.authserver.users.User
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.web.multipart.MultipartFile
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

class FileSystemStorage : FileStorage {
    override fun save(
        user: User,
        path: String,
        file: MultipartFile
    ): String {
        val root = Paths.get(ROOT)
        val destinationFile = root.resolve(path)
            .normalize()
            .toAbsolutePath()
        Files.createDirectories(destinationFile.parent)
        file.inputStream.use {
            Files.copy(
                it, destinationFile,
                StandardCopyOption.REPLACE_EXISTING
            )
        }
        log.info("saved $destinationFile")
        return path
    }

    override fun load(path: String): Resource? =
        Paths.get(ROOT, path.replace("-S-", "/"))
            .takeIf { it.exists() }
            ?.let { UrlResource(it.toUri()) }


    override fun urlFor(name: String): String =
        "http://localhost:8080/api/files/" +
                URLEncoder.encode(
                    name.replace("/", "-S-"),
                    StandardCharsets.UTF_8
                )

    companion object {
        val ROOT = "./fs"
        val log = LoggerFactory.getLogger(FileSystemStorage::class.java)!!
    }
}

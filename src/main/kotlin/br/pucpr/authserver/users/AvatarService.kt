package br.pucpr.authserver.users

import br.pucpr.authserver.exception.UnsupportedMediaTypeException
import br.pucpr.authserver.files.FileStorage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayInputStream
import java.security.MessageDigest

@Service
class AvatarService(
    @param:Qualifier("fileStorage") val storage: FileStorage,
    val webClientBuilder: WebClient.Builder,
    val userRepository: UserRepository
) {
    private val GRAVATAR_URL = "https://www.gravatar.com/avatar/"
    private val UI_AVATARS_URL = "https://ui-avatars.com/api/?name="

    fun saveDefaultAvatar(user: User): String {

        val avatarBytes = user.email.takeIf { it.isNotBlank() }?.let { getGravatarImage(it) }
            ?: getUiAvatarImage(user.name)

        return saveImageToStorage(user, avatarBytes)
    }

    private fun getGravatarImage(email: String): ByteArray? {
        val emailHash = md5Hex(email.trim().lowercase())
        val url = "${GRAVATAR_URL}$emailHash?d=404&s=200"

        return try {
            webClientBuilder.build().get().uri(url)
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .block()
        } catch (e: Exception) {
            log.warn("Gravatar não encontrado ou falhou. Passando para UI-Avatars.")
            null
        }
    }

    fun deleteUserAvatar(userId: Long) {

        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário $userId não encontrado.") }
        val avatarPath = user.avatar
        if (avatarPath.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "O usuário $userId não possui um avatar para ser removido.")
        }

        try {
            storage.remove(avatarPath)
        } catch (e: Exception) {
            log.error("Falha ao deletar objeto S3 para usuário ${userId}. Path: $avatarPath", e)

            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Falha na comunicação com o S3 durante a exclusão. Verifique logs para detalhes de permissão.", e)
        }

        user.avatar = DEFAULT_AVATAR
        userRepository.save(user)
    }

    private fun getUiAvatarImage(name: String): ByteArray {
        val encodedName = name.replace(" ", "+")
        val url = "${UI_AVATARS_URL}$encodedName&size=200&background=random&color=ffffff&format=png"

        return webClientBuilder.build().get().uri(url)
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block()!!
    }

    private fun md5Hex(str: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(str.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }


    private fun getStoragePath(userId: Long): String = "${FOLDER}/$userId/avatar.png"

    private fun saveImageToStorage(user: User, imageBytes: ByteArray): String {
        val path = getStoragePath(user.id!!)

        val multipartFile = InMemoryMultipartFile(
            name = "avatar",
            filename = "avatar.png",
            contentType = "image/png",
            bytes = imageBytes
        )

        return storage.save(user, path, multipartFile)
    }


    fun save(user: User, avatar: MultipartFile): String =
        try {
            val contentType = avatar.contentType!!
            val extension = when (contentType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                else -> throw UnsupportedMediaTypeException("jpg", "png")
            }
            val name = "${user.id}/avatar.$extension"
            val path = "$FOLDER/$name"
            storage.save(user, path, avatar)
            name
        } catch (e: Exception) {
            log.error("Error saving avatar for user ${user.id}. Using default.", e)
            DEFAULT_AVATAR
        }

    fun load(name: String) = storage.load(name)
    fun urlFor(path: String) = storage.urlFor(path)

    companion object {
        const val FOLDER = "avatars"
        const val DEFAULT_AVATAR = "default_avatar.jpg"
        private val log = LoggerFactory.getLogger(AvatarService::class.java)
        class InMemoryMultipartFile(
            private val name: String,
            private val filename: String,
            private val contentType: String,
            private val bytes: ByteArray
        ) : MultipartFile {
            override fun getName(): String = name
            override fun getOriginalFilename(): String? = filename
            override fun getContentType(): String? = contentType
            override fun isEmpty(): Boolean = bytes.isEmpty()
            override fun getSize(): Long = bytes.size.toLong()
            override fun getBytes(): ByteArray = bytes
            override fun getInputStream(): ByteArrayInputStream = ByteArrayInputStream(bytes)
            override fun transferTo(dest: java.io.File) {
                throw UnsupportedOperationException("Not supported")
            }
        }
    }
}
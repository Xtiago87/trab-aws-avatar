package br.pucpr.authserver.users

import br.pucpr.authserver.exception.UnsupportedMediaTypeException
import br.pucpr.authserver.files.FileStorage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class AvatarService(
    @param:Qualifier("fileStorage") val storage: FileStorage
) {
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

    fun urlFor(path: String) = storage.urlFor("$FOLDER/$path")

    companion object {
        const val FOLDER = "avatars"
        const val DEFAULT_AVATAR = "default.jpg"
        private val log = LoggerFactory.getLogger(AvatarService::class.java)
    }
}

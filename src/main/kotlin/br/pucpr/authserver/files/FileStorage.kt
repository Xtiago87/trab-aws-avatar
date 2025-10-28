package br.pucpr.authserver.files

import br.pucpr.authserver.users.User
import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile

interface FileStorage {
    fun save(user: User, path: String, file: MultipartFile): String
    fun load(path: String): Resource?
    fun urlFor(name: String): String
}

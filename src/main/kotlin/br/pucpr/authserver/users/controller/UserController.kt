package br.pucpr.authserver.users.controller

import br.pucpr.authserver.exception.ForbiddenException
import br.pucpr.authserver.security.UserToken
import br.pucpr.authserver.users.SortDir
import br.pucpr.authserver.users.UserService
import br.pucpr.authserver.users.controller.requests.CreateUserRequest
import br.pucpr.authserver.users.controller.requests.LoginRequest
import br.pucpr.authserver.users.controller.requests.PatchUserRequest
import br.pucpr.authserver.users.controller.responses.UserResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/users")
class UserController(val service: UserService) {
    @PostMapping
    fun insert(@Valid @RequestBody user: CreateUserRequest) =
        service.insert(user.toUser())
            .let { service.toResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @SecurityRequirement(name = "AuthServer")
    @PreAuthorize("permitAll()")
    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: PatchUserRequest,
        auth: Authentication
    ): ResponseEntity<UserResponse> {
        val token = auth.principal as? UserToken ?: throw ForbiddenException()
        if (token.id != id && !token.isAdmin) throw ForbiddenException()

        return service.update(id, request.name!!)
            ?.let { ResponseEntity.ok(service.toResponse(it)) }
            ?: ResponseEntity.noContent().build()
    }

    @GetMapping
    fun list(@RequestParam sortDir: String? = null, @RequestParam role: String? = null) =
        if (role == null) {
            service.findAll(SortDir.findOrThrow(sortDir ?: "ASC"))
        } else {
            service.findByRole(role.uppercase())
        }.map { service.toResponse(it) }.let { ResponseEntity.ok(it) }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) =
        service.findByIdOrNull(id)
            ?.let { ResponseEntity.ok(service.toResponse(it)) }
            ?: ResponseEntity.notFound().build()

    @SecurityRequirement(name = "AuthServer")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> =
        if (service.delete(id)) ResponseEntity.ok().build()
        else ResponseEntity.notFound().build()

    @PutMapping("/{id}/roles/{role}")
    fun grant(@PathVariable id: Long, @PathVariable role: String): ResponseEntity<Void> =
        if (service.addRole(id, role.uppercase())) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.noContent().build()
        }

    @PostMapping("/login")
    fun login(@Valid @RequestBody login: LoginRequest) =
        service.login(login.email!!, login.password!!)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

    @SecurityRequirement(name = "AuthServer")
    @PreAuthorize("permitAll()")
    @PutMapping("/{id}/avatar", consumes = ["multipart/form-data"])
    fun uploadAvatar(@PathVariable id: Long, @RequestParam avatar: MultipartFile) =
        service.saveAvatar(id, avatar)
            .also { ResponseEntity.ok().build<Void>() }

}

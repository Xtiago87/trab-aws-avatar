package br.pucpr.authserver

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class RootController {
    @GetMapping("healthcheck")
    fun healthCheck() = ResponseEntity.ok(mapOf("status" to "OK"))
}

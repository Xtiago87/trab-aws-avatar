package br.pucpr.authserver.files

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class FilesConfig {
    @Bean("fileStorage")
    @Profile("!fs")
    fun s3Storage() = S3Storage()

    @Bean("fileStorage")
    @Profile("fs")
    fun localStorage() = FileSystemStorage()
}

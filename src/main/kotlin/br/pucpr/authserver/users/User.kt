package br.pucpr.authserver.users


import br.pucpr.authserver.roles.Role
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(name = "TblUser")
class User(
    @Id @GeneratedValue
    var id: Long? = null,

    @Column(unique = true)
    var email: String = "",

    var password: String = "",

    var name: String = "",

    var avatar: String = AvatarService.DEFAULT_AVATAR,

    @ManyToMany
    @JoinTable(
        name = "UserRole",
        joinColumns = [JoinColumn(name = "idUser")],
        inverseJoinColumns = [JoinColumn(name = "idRole")]
    )
    val roles: MutableSet<Role> = mutableSetOf()
) {
    @get:JsonIgnore
    @get:Transient
    val isAdmin: Boolean get() = roles.any { it.name == "ADMIN" }
}

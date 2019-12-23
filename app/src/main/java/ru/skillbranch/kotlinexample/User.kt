package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null

    private var _login: String? = null
    var login: String
        set(value) {
            _login = value?.toLowerCase()
        }
        get() = _login!!

    //    private var _salt: String by lazy {
//        ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
//    }
    private var _salt: String? = null
    private var salt: String
        set(value) {
            _salt = value
        }
        get() {
            if(_salt.isNullOrBlank()){
                _salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
            }
            return _salt!!
        }


    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null


    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("constructor(firstName: $firstName, lastName: $lastName, email: $email, password: $password)")
        passwordHash = encrypt(password)
    }

    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        salt: String?,
        hash: String?,
        rawPhone: String?
    ) : this(
        firstName,
        lastName,
        email = email,
        rawPhone = rawPhone,
        meta = mapOf("src" to "csv")
    ) {
        println("constructor(firstName: $firstName, lastName: $lastName, email: $email, salt: $salt, hash: $hash, rawPhone: $rawPhone)")
        if (!hash.isNullOrBlank() && !salt.isNullOrBlank()) {
            passwordHash = hash
            this.salt = salt
        }
    }


    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("constructor(firstName: $firstName, $lastName: lastName, rawPhone: $rawPhone)")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code

        sendAccessCodeToUser(phone, code)
    }


    init {
        println("First init block")

        check(!firstName.isBlank()) { "FirstName must be not blank" }
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email or phone must be not blank" }

        phone = rawPhone?.checkPhone()
        login = email ?: phone!!

        userInfo = """
              firstName: $firstName
              lastName: $lastName
              login: $login
              fullName: $fullName
              initials: $initials
              email: $email
              phone: $phone
              meta: $meta
            """.trimIndent()
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not match the current password")
    }

    fun requestAccessCode() {
        println("requestAccessCode()")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        println("generated code: $code")
    }

    private fun encrypt(password: String): String = salt.plus(password).md5()

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun String.md5(): String {
        val md: MessageDigest = MessageDigest.getInstance("MD5")
        val digest: ByteArray = md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    private fun sendAccessCodeToUser(phone: String?, code: String) {
        println("... sending access code: $code on $phone")
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            rawPhone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !rawPhone.isNullOrBlank() -> User(firstName, lastName, rawPhone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    password
                )
                else -> throw IllegalArgumentException("Email or phone must be not null")
            }
        }

        fun makeUserFromCsv(
            fullName: String,
            email: String? = null,
            salt: String? = null,
            hash: String? = null,
            rawPhone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return User(
                firstName,
                lastName,
                email,
                salt,
                hash,
                rawPhone
            )
        }


        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException(
                            "Fullname must contain only first name " +
                                    "and last name, current split result ${this@fullNameToPair}"
                        )
                    }
                }
        }

        private fun String.checkPhone(): String? {
            val checked = !this.isNullOrBlank()
                    && this[0] == '+'
                    && this?.replace("[^+\\d]".toRegex(), "").length == 12
                    && this.replace("[\\W\\d]".toRegex(), "").isEmpty()

            if (!checked) throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")

            return this?.replace("[^+\\d]".toRegex(), "")

        }
    }
}



package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(fullName: String, email: String, password: String): User {
        return User.makeUser(fullName, email, password)
            .also { user ->
                if (map.containsKey(user.login))
                    throw IllegalArgumentException("A user with this login already exists")
                map[user.login] = user
            }
    }

    fun registerUserByLoginCsv(
        fullName: String,
        email: String?,
        salt: String?,
        hash: String?,
        rawPhone: String?
    ): User {
        return User.makeUserFromCsv(fullName, email, salt, hash, rawPhone)
            .also { user ->
                if (map.containsKey(user.login))
                    throw IllegalArgumentException("A user with this login already exists")
                map[user.login] = user
            }
    }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        return User.makeUser(fullName, rawPhone = rawPhone)
            .also { user ->
                if (map.containsKey(user.login))
                    throw IllegalArgumentException("A user with this login already exists")
                map[user.login] = user
            }
    }

    fun loginUser(login: String, password: String): String? {
        return map[login.trim()]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    fun requestAccessCode(login: String): Unit {
        map[login.trim()]?.requestAccessCode()
    }

    //    Полное имя пользователя; email; соль:хеш пароля; телефон
//    (Пример: " John Doe ;JohnDoe@unknow.com;[B@7591083d:c6adb4becdc64e92857e1e2a0fd6af84;;"
    fun importUsers(list: List<String>): List<User> {
        val users = mutableListOf<User>()

        list.forEach {
            val data = it.splitIgnoreEmpty(";")

            val creds = data[2]?.split(":")
            registerUserByLoginCsv(
                data[0]!!.trim(),
                data[1]?.trim(),
                creds?.get(0)?.trim(),
                creds?.get(1)?.trim(),
                data[3]?.trim()
            )

        }
        return users
    }

    fun CharSequence.splitIgnoreEmpty(delimiter: String): List<String?> {
        return this.split(delimiter).map {
            if(it.isEmpty()) null else it
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

}
package edu.sswu.vitaday

import android.app.Application

class App : Application() {

    // DB와 Repository를 앱 전체에서 공유
    val database by lazy { UserDatabase.getDatabase(this) }
    val repository by lazy { UserRepository(database.userDao()) }
}

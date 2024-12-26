package com.OxGames.Pluvia.di

import android.content.Context
import androidx.room.Room
import com.OxGames.Pluvia.db.DATABASE_NAME
import com.OxGames.Pluvia.db.PluviaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PluviaDatabase {
        // The db will be considered unstable during development.
        // Once stable we should add a (room) db migration
        return Room.databaseBuilder(context, PluviaDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration() // TODO remove before prod
            .build()
    }

    @Provides
    @Singleton
    fun provideSteamFriendDao(db: PluviaDatabase) = db.steamFriendDao()
}

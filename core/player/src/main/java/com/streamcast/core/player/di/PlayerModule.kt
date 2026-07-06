package com.streamcast.core.player.di

import com.streamcast.core.player.ExoPlayerManagerImpl
import com.streamcast.core.player.PlayerManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindPlayerManager(
        exoPlayerManagerImpl: ExoPlayerManagerImpl
    ): PlayerManager
}

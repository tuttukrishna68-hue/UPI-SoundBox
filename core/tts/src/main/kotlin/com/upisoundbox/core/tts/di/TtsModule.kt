package com.upisoundbox.core.tts.di

import android.content.Context
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.tts.AnnouncementEngine
import com.upisoundbox.core.tts.AnnouncementQueue
import com.upisoundbox.core.tts.AudioChimePlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TtsModule {

    @Provides
    @Singleton
    fun provideAudioChimePlayer(): AudioChimePlayer = AudioChimePlayer()

    @Provides
    @Singleton
    fun provideAnnouncementEngine(
        @ApplicationContext context: Context,
        chimePlayer: AudioChimePlayer
    ): AnnouncementEngine {
        return AnnouncementEngine(context, chimePlayer)
    }

    @Provides
    @Singleton
    fun provideAnnouncementQueue(
        engine: AnnouncementEngine,
        preferences: SoundboxPreferences
    ): AnnouncementQueue {
        return AnnouncementQueue(engine, preferences)
    }
}


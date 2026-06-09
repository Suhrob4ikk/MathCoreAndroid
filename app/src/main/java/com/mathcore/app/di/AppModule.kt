package com.mathcore.app.di

import android.content.Context
import com.mathcore.app.data.QuestionRepository
import com.mathcore.app.data.local.PreferencesManager
import com.mathcore.app.data.repository.AuthRepository
import com.mathcore.app.data.repository.MistakesRepository
import com.mathcore.app.data.repository.ResultsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * [А-1] Hilt application-level dependency module.
 *
 * Provides singleton instances of all repositories and managers.
 * Using [SingletonComponent] ensures a single instance per process lifetime,
 * matching the previous manual singleton behaviour in each ViewModel/Activity.
 *
 * Constructor injection is used for classes that need [Context] (annotated with
 * [@ApplicationContext][ApplicationContext] to prevent Activity-Context leaks).
 * Classes with no external deps (ResultsRepository, MistakesRepository) are
 * provided by zero-arg factory calls.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext ctx: Context
    ): PreferencesManager = PreferencesManager(ctx)

    @Provides
    @Singleton
    fun provideQuestionRepository(
        @ApplicationContext ctx: Context
    ): QuestionRepository = QuestionRepository(ctx)

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext ctx: Context
    ): AuthRepository = AuthRepository(ctx)

    @Provides
    @Singleton
    fun provideResultsRepository(): ResultsRepository = ResultsRepository()

    @Provides
    @Singleton
    fun provideMistakesRepository(): MistakesRepository = MistakesRepository()
}

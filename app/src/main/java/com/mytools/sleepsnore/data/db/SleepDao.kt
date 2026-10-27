package com.mytools.sleepsnore.data.db

import androidx.room.*
import com.mytools.sleepsnore.data.model.AudioSample
import com.mytools.sleepsnore.data.model.ChartPoint
import com.mytools.sleepsnore.data.model.SleepSession
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {

    // Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SleepSession>)

    @Update
    suspend fun updateSession(session: SleepSession)

    @Delete
    suspend fun deleteSession(session: SleepSession)

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SleepSession>>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsSync(): List<SleepSession>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): SleepSession?

    @Query("SELECT * FROM sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSession(): SleepSession?

    @Query("SELECT * FROM sessions ORDER BY startTime DESC LIMIT 1")
    fun getLatestSessionFlow(): Flow<SleepSession?>

    @Query("UPDATE sessions SET restRating = :rating, notes = :notes WHERE id = :sessionId")
    suspend fun updateSessionRatingAndNotes(sessionId: Long, rating: Int, notes: String)

    // Chart Points (30s Epochs)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChartPoint(point: ChartPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChartPoints(points: List<ChartPoint>)

    @Query("SELECT * FROM chart_points WHERE sessionId = :sessionId ORDER BY offsetSeconds ASC")
    fun getChartPointsBySession(sessionId: Long): Flow<List<ChartPoint>>

    @Query("SELECT * FROM chart_points WHERE sessionId = :sessionId ORDER BY offsetSeconds ASC")
    suspend fun getChartPointsSync(sessionId: Long): List<ChartPoint>

    // Audio Samples
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioSample(sample: AudioSample): Long

    @Query("SELECT * FROM audio_samples WHERE sessionId = :sessionId ORDER BY offsetSeconds ASC")
    fun getAudioSamplesBySession(sessionId: Long): Flow<List<AudioSample>>

    @Query("SELECT * FROM audio_samples ORDER BY timestampMs DESC")
    fun getAllAudioSamples(): Flow<List<AudioSample>>

    @Query("UPDATE audio_samples SET isFavorite = :isFav WHERE id = :sampleId")
    suspend fun setSampleFavorite(sampleId: Long, isFav: Boolean)

    @Delete
    suspend fun deleteAudioSample(sample: AudioSample)

    @Query("DELETE FROM audio_samples WHERE id = :sampleId")
    suspend fun deleteAudioSampleById(sampleId: Long)
}

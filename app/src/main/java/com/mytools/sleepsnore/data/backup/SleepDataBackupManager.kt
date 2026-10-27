package com.mytools.sleepsnore.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.mytools.sleepsnore.data.db.SleepDao
import com.mytools.sleepsnore.data.model.ChartPoint
import com.mytools.sleepsnore.data.model.SleepSession
import com.mytools.sleepsnore.data.model.SnoreIntensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 全量睡眠数据跨设备导入/导出管理器
 * 支持 JSON 结构化整机备份与恢复、CSV 科研与 Excel 报表导出
 * 基于 Android 原生 org.json 与 FileProvider，零额外第三方依赖
 */
object SleepDataBackupManager {

    suspend fun exportJsonBackup(context: Context, dao: SleepDao): Uri? = withContext(Dispatchers.IO) {
        try {
            val sessions = dao.getAllSessionsSync()
            val root = JSONObject()
            root.put("version", 3)
            root.put("exportTime", System.currentTimeMillis())

            val sessionsArray = JSONArray()
            for (s in sessions) {
                val sObj = JSONObject().apply {
                    put("id", s.id)
                    put("startTime", s.startTime)
                    put("endTime", s.endTime)
                    put("sessionDurationSeconds", s.sessionDurationSeconds)
                    put("snoringDurationSeconds", s.snoringDurationSeconds)
                    put("snoreScore", s.snoreScore.toDouble())
                    put("quietPercent", s.quietPercent.toDouble())
                    put("mildPercent", s.mildPercent.toDouble())
                    put("loudPercent", s.loudPercent.toDouble())
                    put("epicPercent", s.epicPercent.toDouble())
                    put("minVolumeDb", s.minVolumeDb.toDouble())
                    put("maxVolumeDb", s.maxVolumeDb.toDouble())
                    put("avgVolumeDb", s.avgVolumeDb.toDouble())
                    put("restRating", s.restRating)
                    put("notes", s.notes)
                    put("apneaCount", s.apneaCount)
                    put("estimatedAhi", s.estimatedAhi.toDouble())
                    put("palatalPercent", s.palatalPercent.toDouble())
                    put("tongueBasePercent", s.tongueBasePercent.toDouble())
                    put("gaspPercent", s.gaspPercent.toDouble())

                    val fArray = JSONArray()
                    s.factorIds.forEach { fArray.put(it) }
                    put("factorIds", fArray)

                    val rArray = JSONArray()
                    s.remedyIds.forEach { rArray.put(it) }
                    put("remedyIds", rArray)
                }
                sessionsArray.put(sObj)
            }
            root.put("sessions", sessionsArray)

            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(context.cacheDir, "SleepSnore_Backup_$timeStr.json")
            FileOutputStream(exportFile).use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportCsvSummary(context: Context, dao: SleepDao): Uri? = withContext(Dispatchers.IO) {
        try {
            val sessions = dao.getAllSessionsSync()
            val sb = StringBuilder()
            // CSV Header
            sb.append("会话ID,开始时间,结束时间,在床时长(秒),打鼾时长(秒),鼾声评分(SnoreScore),安静占比(%),轻度占比(%),大声占比(%),超大声占比(%),呼吸暂停次数,预估AHI,晨起精力(星级),睡后备忘\n")

            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            for (s in sessions) {
                sb.append(s.id).append(",")
                sb.append("\"").append(df.format(Date(s.startTime))).append("\",")
                sb.append("\"").append(if (s.endTime > 0) df.format(Date(s.endTime)) else "").append("\",")
                sb.append(s.sessionDurationSeconds).append(",")
                sb.append(s.snoringDurationSeconds).append(",")
                sb.append(String.format(Locale.US, "%.1f", s.snoreScore)).append(",")
                sb.append(String.format(Locale.US, "%.1f", s.quietPercent * 100f)).append(",")
                sb.append(String.format(Locale.US, "%.1f", s.mildPercent * 100f)).append(",")
                sb.append(String.format(Locale.US, "%.1f", s.loudPercent * 100f)).append(",")
                sb.append(String.format(Locale.US, "%.1f", s.epicPercent * 100f)).append(",")
                sb.append(s.apneaCount).append(",")
                sb.append(String.format(Locale.US, "%.1f", s.estimatedAhi)).append(",")
                sb.append(s.restRating).append(",")
                sb.append("\"").append(s.notes.replace("\"", "\"\"")).append("\"\n")
            }

            val timeStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val csvFile = File(context.cacheDir, "SleepSnore_Report_$timeStr.csv")
            FileOutputStream(csvFile).use {
                // BOM for Excel UTF-8 display
                it.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                it.write(sb.toString().toByteArray(Charsets.UTF_8))
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun importJsonBackup(context: Context, uri: Uri, dao: SleepDao): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext Result.failure(Exception("无法读取备份文件内容"))

            val root = JSONObject(content)
            val sessionsArray = root.optJSONArray("sessions") ?: return@withContext Result.failure(Exception("未在文件中找到有效睡眠会话列表"))

            val restoredSessions = mutableListOf<SleepSession>()
            for (i in 0 until sessionsArray.length()) {
                val obj = sessionsArray.getJSONObject(i)

                val factors = mutableListOf<String>()
                obj.optJSONArray("factorIds")?.let { arr ->
                    for (j in 0 until arr.length()) factors.add(arr.getString(j))
                }

                val remedies = mutableListOf<String>()
                obj.optJSONArray("remedyIds")?.let { arr ->
                    for (j in 0 until arr.length()) remedies.add(arr.getString(j))
                }

                val session = SleepSession(
                    id = obj.optLong("id", 0),
                    startTime = obj.optLong("startTime", System.currentTimeMillis()),
                    endTime = obj.optLong("endTime", 0),
                    sessionDurationSeconds = obj.optLong("sessionDurationSeconds", 0),
                    snoringDurationSeconds = obj.optLong("snoringDurationSeconds", 0),
                    snoreScore = obj.optDouble("snoreScore", 0.0).toFloat(),
                    quietPercent = obj.optDouble("quietPercent", 1.0).toFloat(),
                    mildPercent = obj.optDouble("mildPercent", 0.0).toFloat(),
                    loudPercent = obj.optDouble("loudPercent", 0.0).toFloat(),
                    epicPercent = obj.optDouble("epicPercent", 0.0).toFloat(),
                    minVolumeDb = obj.optDouble("minVolumeDb", 0.0).toFloat(),
                    maxVolumeDb = obj.optDouble("maxVolumeDb", 0.0).toFloat(),
                    avgVolumeDb = obj.optDouble("avgVolumeDb", 0.0).toFloat(),
                    factorIds = factors,
                    remedyIds = remedies,
                    restRating = obj.optInt("restRating", 0),
                    notes = obj.optString("notes", ""),
                    apneaCount = obj.optInt("apneaCount", 0),
                    estimatedAhi = obj.optDouble("estimatedAhi", 0.0).toFloat(),
                    palatalPercent = obj.optDouble("palatalPercent", 0.0).toFloat(),
                    tongueBasePercent = obj.optDouble("tongueBasePercent", 0.0).toFloat(),
                    gaspPercent = obj.optDouble("gaspPercent", 0.0).toFloat()
                )
                restoredSessions.add(session)
            }

            if (restoredSessions.isNotEmpty()) {
                dao.insertSessions(restoredSessions)
            }

            Result.success(restoredSessions.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

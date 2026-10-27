package com.mytools.sleepsnore.report

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.mytools.sleepsnore.data.model.ChartPoint
import com.mytools.sleepsnore.data.model.SleepSession
import com.mytools.sleepsnore.data.model.SnoreIntensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    suspend fun generateAndShareReport(
        context: Context,
        session: SleepSession,
        chartPoints: List<ChartPoint>
    ) = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 (595 x 842 pt)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
        }

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateOnlyFormatter = SimpleDateFormat("yyyy年M月d日", Locale.CHINESE)

        // 1. 顶部 Header 装饰栏
        paint.color = Color.parseColor("#153066")
        canvas.drawRect(0f, 0f, 595f, 75f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SnoreLab 睡眠健康与打鼾分析报告", 35f, 42f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#90CAF9")
        canvas.drawText("CLINICAL SLEEP & SNORE ANALYSIS REPORT · 呼吸与声学特征诊断参考", 35f, 60f, paint)

        // 2. 检测基本参数
        paint.color = Color.parseColor("#333333")
        paint.textSize = 11f
        var currentY = 105f
        val startStr = dateFormatter.format(Date(session.startTime))
        val endStr = if (session.endTime > 0) dateFormatter.format(Date(session.endTime)) else "监测结束"

        canvas.drawText("检测日期: ${dateOnlyFormatter.format(Date(session.startTime))}", 35f, currentY, paint)
        canvas.drawText("监测时间段: $startStr ~ $endStr", 220f, currentY, paint)
        currentY += 20f

        val totalH = session.sessionDurationSeconds / 3600
        val totalM = (session.sessionDurationSeconds % 3600) / 60
        val snoreH = session.snoringDurationSeconds / 3600
        val snoreM = (session.snoringDurationSeconds % 3600) / 60
        val snorePct = if (session.sessionDurationSeconds > 0) (session.snoringDurationSeconds * 100 / session.sessionDurationSeconds) else 0

        canvas.drawText("总卧床时间: ${totalH}小时${totalM}分", 35f, currentY, paint)
        canvas.drawText("总打鼾时间: ${snoreH}小时${snoreM}分 (占比 ${snorePct}%)", 220f, currentY, paint)
        canvas.drawText("峰值分贝: ${session.maxVolumeDb.toInt()} dB", 430f, currentY, paint)
        currentY += 25f

        // 分割线
        paint.color = Color.parseColor("#E0E0E0")
        canvas.drawLine(35f, currentY, 560f, currentY, paint)
        currentY += 25f

        // 3. 核心评估双看板 (打鼾评分 + 呼吸暂停指数)
        // 左看板：打鼾评分
        paint.color = Color.parseColor("#F5F7FA")
        canvas.drawRoundRect(35f, currentY, 285f, currentY + 95f, 12f, 12f, paint)
        paint.color = Color.parseColor("#666666")
        paint.textSize = 11f
        canvas.drawText("打鼾严重度评分 (Snore Score)", 50f, currentY + 24f, paint)

        paint.color = Color.parseColor("#153066")
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${session.snoreScore.toInt()}", 50f, currentY + 65f, paint)

        val scoreLevel = when {
            session.snoreScore < 15f -> "正常 / 轻度"
            session.snoreScore < 30f -> "轻声打鼾"
            session.snoreScore < 70f -> "中度大声打鼾"
            else -> "重度极响打鼾"
        }
        paint.textSize = 12f
        paint.color = Color.parseColor("#2599CC")
        canvas.drawText("评级: $scoreLevel", 120f, currentY + 60f, paint)

        // 右看板：呼吸暂停 (Apnea & AHI)
        paint.color = Color.parseColor("#F5F7FA")
        canvas.drawRoundRect(310f, currentY, 560f, currentY + 95f, 12f, 12f, paint)
        paint.color = Color.parseColor("#666666")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("疑似睡眠呼吸暂停 (Apnea / AHI)", 325f, currentY + 24f, paint)

        paint.color = if (session.apneaCount > 5) Color.parseColor("#D1693C") else Color.parseColor("#153066")
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${session.apneaCount}", 325f, currentY + 65f, paint)

        val ahi = session.estimatedAhi
        val ahiDesc = when {
            ahi < 5f -> "AHI预估 <5 (正常)"
            ahi < 15f -> "AHI预估 ${"%.1f".format(ahi)} (轻度OSA风险)"
            ahi < 30f -> "AHI预估 ${"%.1f".format(ahi)} (中度OSA风险)"
            else -> "AHI预估 ${"%.1f".format(ahi)} (重度OSA高危)"
        }
        paint.textSize = 11f
        paint.color = if (ahi >= 5f) Color.parseColor("#D1693C") else Color.parseColor("#488E83")
        canvas.drawText(ahiDesc, 395f, currentY + 60f, paint)

        currentY += 120f

        // 4. 四分级时间占比横条
        paint.color = Color.parseColor("#333333")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("打鼾强度四分级耗时分布", 35f, currentY, paint)
        currentY += 14f

        val barX = 35f
        val barTotalW = 525f
        val barH = 14f
        var curX = barX

        // 绘制四色分布条
        val segments = listOf(
            session.quietPercent to Color.parseColor("#344E59"),
            session.mildPercent to Color.parseColor("#488E83"),
            session.loudPercent to Color.parseColor("#C9A36E"),
            session.epicPercent to Color.parseColor("#D1693C")
        )
        for ((pct, color) in segments) {
            val w = pct * barTotalW
            if (w > 0.5f) {
                paint.color = color
                canvas.drawRect(curX, currentY, curX + w, currentY + barH, paint)
                curX += w
            }
        }
        currentY += 28f

        // 四级指标图例文字
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#344E59")
        canvas.drawText("■ 安静: ${(session.quietPercent * 100).toInt()}%", 35f, currentY, paint)
        paint.color = Color.parseColor("#488E83")
        canvas.drawText("■ 轻声: ${(session.mildPercent * 100).toInt()}%", 160f, currentY, paint)
        paint.color = Color.parseColor("#C9A36E")
        canvas.drawText("■ 大声: ${(session.loudPercent * 100).toInt()}%", 295f, currentY, paint)
        paint.color = Color.parseColor("#D1693C")
        canvas.drawText("■ 超大声: ${(session.epicPercent * 100).toInt()}%", 430f, currentY, paint)
        currentY += 30f

        // 5. 整夜时间轴柱状直方图微缩版
        paint.color = Color.parseColor("#333333")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("整夜声音时间轴直方图 (SnoreGraph)", 35f, currentY, paint)
        currentY += 14f

        // 图表背景
        paint.color = Color.parseColor("#181B21")
        canvas.drawRoundRect(35f, currentY, 560f, currentY + 110f, 8f, 8f, paint)

        if (chartPoints.isNotEmpty()) {
            val chartW = 515f
            val chartH = 90f
            val chartLeft = 40f
            val chartTop = currentY + 10f
            val count = chartPoints.size
            val barW = (chartW / count.toFloat()).coerceAtLeast(0.8f)

            for (i in 0 until count) {
                val pt = chartPoints[i]
                val normDb = ((pt.volumeDb - 30f) / 55f).coerceIn(0.08f, 1f)
                val h = chartH * normDb
                val x = chartLeft + i * barW
                val y = chartTop + chartH - h

                paint.color = when (pt.intensityLevel) {
                    SnoreIntensity.QUIET -> Color.parseColor("#344E59")
                    SnoreIntensity.MILD -> Color.parseColor("#488E83")
                    SnoreIntensity.LOUD -> Color.parseColor("#C9A36E")
                    SnoreIntensity.EPIC -> Color.parseColor("#D1693C")
                }
                canvas.drawRect(x, y, x + barW, chartTop + chartH, paint)
            }
        }
        currentY += 130f

        // 6. 对策与生活诱因记录
        paint.color = Color.parseColor("#333333")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("防鼾干预与生活习惯记录", 35f, currentY, paint)
        currentY += 18f

        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#555555")
        val remediesText = if (session.remedyIds.isEmpty()) "未采用任何防鼾用品" else session.remedyIds.joinToString("、 ")
        val factorsText = if (session.factorIds.isEmpty()) "无特殊生活诱因记录" else session.factorIds.joinToString("、 ")
        canvas.drawText("采用止鼾对策: $remediesText", 35f, currentY, paint)
        currentY += 18f
        canvas.drawText("睡前影响因素: $factorsText", 35f, currentY, paint)
        currentY += 18f
        if (session.notes.isNotBlank()) {
            canvas.drawText("晨起备忘与精力: ${session.notes} (自评: ${session.restRating}星)", 35f, currentY, paint)
            currentY += 18f
        }

        // 7. 底部医学提示与免责声明
        currentY = 780f
        paint.color = Color.parseColor("#E0E0E0")
        canvas.drawLine(35f, currentY, 560f, currentY, paint)
        currentY += 16f

        paint.color = Color.parseColor("#888888")
        paint.textSize = 9f
        canvas.drawText("免责声明：本报告由智能音频声学分析生成，仅供睡眠打鼾特征观察与改善参考，不可代替医院多导睡眠监测 (PSG) 临床医疗诊断。", 35f, currentY, paint)
        currentY += 12f
        canvas.drawText("若您夜间频繁憋醒、晨起头痛或日间重度嗜睡，建议携带本报告前往医院耳鼻喉科 (ENT) 或呼吸睡眠中心就诊。", 35f, currentY, paint)

        pdfDocument.finishPage(page)

        // 导出保存文件
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val pdfFile = File(reportsDir, "SnoreLab_Report_${session.startTime}.pdf")

        val fos = FileOutputStream(pdfFile)
        pdfDocument.writeTo(fos)
        fos.flush()
        fos.close()
        pdfDocument.close()

        // 唤起系统分享
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SnoreLab 睡眠健康与打鼾分析报告")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "导出/分享医学报告 (PDF)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

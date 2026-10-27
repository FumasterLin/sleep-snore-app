package com.mytools.sleepsnore.data.model

import androidx.annotation.DrawableRes
import com.mytools.sleepsnore.R

data class SleepInfluenceItem(
    val id: String,
    val title: String,
    val description: String,
    val isRemedy: Boolean,
    @DrawableRes val imageResId: Int
) {
    companion object {
        val ALL_REMEDIES = listOf(
            SleepInfluenceItem("side_sleeping", "侧卧睡姿", "避免仰卧时悬雍垂与舌根后坠堵塞气道", true, R.drawable.remedy_side_sleeping),
            SleepInfluenceItem("wedge_pillow", "楔形抬高枕", "将头胸部抬高 15~30 度减轻咽部压迫", true, R.drawable.remedy_wedge_pillow),
            SleepInfluenceItem("nasal_strip", "通气鼻贴", "物理扩张鼻翼软骨，改善鼻腔进气阻力", true, R.drawable.remedy_nasal_strip),
            SleepInfluenceItem("mouthpiece", "防鼾牙套", "下颌前移装置，保持下颌微前推打开气道", true, R.drawable.remedy_mouthpiece),
            SleepInfluenceItem("cpap", "呼吸机 (CPAP)", "持续气道正压通气，针对重度 OSA 临床金标准", true, R.drawable.remedy_cpap),
            SleepInfluenceItem("nasal_dilator", "鼻通扩张器", "置入鼻孔内侧，强力撑开狭窄前鼻孔", true, R.drawable.remedy_nasal_dilator),
            SleepInfluenceItem("humidifier", "室内加湿器", "保持卧室空气湿度 50%~60%，缓解黏膜干燥充血", true, R.drawable.remedy_humidifier),
            SleepInfluenceItem("mouth_tape", "闭口呼吸贴", "纠正夜间张口呼吸，恢复健康的鼻腔气流循环", true, R.drawable.remedy_mouth_tape),
            SleepInfluenceItem("smart_nora", "智能防鼾垫", "检测到打鼾时无感微充气调节枕头高度促使侧睡", true, R.drawable.remedy_smart_nora),
            SleepInfluenceItem("nasal_spray", "生理盐水鼻喷", "睡前冲洗软化鼻黏膜分泌物消除水肿", true, R.drawable.remedy_nasal_spray),
            SleepInfluenceItem("air_purifier", "空气净化器", "去除尘螨花粉过敏原，预防气道慢性过敏痉挛", true, R.drawable.remedy_air_purifier),
            SleepInfluenceItem("anti_snore_pillow", "防鼾记忆枕", "贴合颈椎生理曲度，防止颈部过度弯曲压迫喉部", true, R.drawable.remedy_anti_snore_pillow),
            SleepInfluenceItem("tongue_retainer", "舌位保持器", "微负压轻吸固定舌体，防止深睡期舌肌下陷", true, R.drawable.remedy_tongue_retainer),
            SleepInfluenceItem("chin_strap", "下颌固定托带", "弹性绑带托举下颌，防止睡眠张嘴呼吸", true, R.drawable.remedy_chin_strap),
            SleepInfluenceItem("throat_spray", "润喉止鼾喷雾", "植物精油配方润滑松弛的咽喉黏膜减少声带震荡", true, R.drawable.remedy_throat_spray),
            SleepInfluenceItem("neti_pot", "洗鼻壶", "温盐水深层清洗上呼吸道黏膜与副鼻窦", true, R.drawable.remedy_neti_pot),
            SleepInfluenceItem("allergy_relief", "抗过敏对策", "睡前服用抗组胺喷剂预防鼻塞与黏膜肥厚", true, R.drawable.remedy_allergy_relief),
            SleepInfluenceItem("positional_therapy", "体位纠正训练", "利用背部阻隔装置建立规律侧卧肌肉记忆", true, R.drawable.remedy_positional_therapy)
        )

        val ALL_FACTORS = listOf(
            SleepInfluenceItem("alcohol", "饮酒", "酒精中枢抑制作用使咽喉部肌肉过度松弛，大幅加重打鼾", false, R.drawable.factor_alcohol),
            SleepInfluenceItem("caffeine", "咖啡因/浓茶", "兴奋中枢神经扰乱睡眠节律导致微觉醒增加", false, R.drawable.factor_caffeine),
            SleepInfluenceItem("heavy_meal", "睡前饱餐/宵夜", "横膈膜抬高压迫胸腔容积，增加睡眠呼吸阻力", false, R.drawable.factor_heavy_meal),
            SleepInfluenceItem("exhaustion", "极度疲倦", "过度疲惫进入深睡导致全身骨骼肌过度脱力坍塌", false, R.drawable.factor_exhaustion),
            SleepInfluenceItem("blocked_nose", "感冒/鼻塞", "鼻阻力剧增被迫转为口呼吸引起软腭剧烈震动", false, R.drawable.factor_blocked_nose),
            SleepInfluenceItem("worked_out", "剧烈运动", "睡前 2 小时高强度心肺负荷导致核心体温升高", false, R.drawable.factor_worked_out),
            SleepInfluenceItem("smoking", "吸烟", "烟焦油刺激气道黏膜充血水肿并损害纤毛摆动", false, R.drawable.factor_smoking),
            SleepInfluenceItem("bath", "睡前泡澡", "促进外周血管扩张助眠，促进核心体温平缓下降", false, R.drawable.factor_bath),
            SleepInfluenceItem("shower", "热水淋浴", "轻度温水放松背部肌群，改善胸廓舒展度", false, R.drawable.factor_shower),
            SleepInfluenceItem("sick", "生病/发热", "全身炎性水肿伴随呼吸急促和呼吸道黏膜渗出", false, R.drawable.factor_sick),
            SleepInfluenceItem("not_my_bed", "出差/非自家床", "环境改变产生首夜效应 (First-Night Effect)", false, R.drawable.factor_not_my_bed),
            SleepInfluenceItem("dehydrated", "身体脱水", "口腔及咽部黏稠分泌物加剧软腭摩擦震动", false, R.drawable.factor_dehydrated),
            SleepInfluenceItem("fasting", "轻断食/空腹", "胃肠排空完全，夜间膈肌无下部压迫感", false, R.drawable.factor_fasting),
            SleepInfluenceItem("sedatives", "镇静/助眠药", "药物松弛舌骨周围悬雍垂肌群，加深气道塌陷", false, R.drawable.factor_sedatives)
        )
    }
}

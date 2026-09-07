# Project Overview
牛马驱动器是以本地计算为核心的 Android 实时收入计时器，产品需求以根目录 PRD 为准。
# Tech Stack
Kotlin / Compose Material 3 / Navigation / ViewModel / Flow / Room / DataStore；纯 JVM domain 模块。
# Important Business Rules
时间是唯一事实来源；午休不计薪；法定调休覆盖普通周规则；金额用 BigDecimal；不保存累计金额；仅日历更新允许联网，不上传用户数据。
# Architecture
:domain 为业务计算，:app 为本地数据、状态与 UI。详见 docs/ARCHITECTURE.md。
# Before Coding
修改业务代码前必须先阅读 docs/PRODUCT.md 与 docs/DOMAIN_RULES.md；日历、退休、存储或 UI 修改时另读对应文档。
# Coding Rules
Calculator 接收 Clock 或当前时间，domain 禁止依赖 Android/Compose；不要加入后台计时服务和网络 SDK。
# Testing Rules
修改算法必须补充确定性 JVM 单测。交付前运行 :domain:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug；记录实际结果，不能把未运行写成通过。
# Documentation Rules
业务、架构、数据变动分别更新对应文档；每阶段更新 CHANGELOG.md 与 TODO.md。

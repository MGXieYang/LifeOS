# Project Overview
个人 LifeOS 是围绕工作与收入、消费与价值、人生时间的 Android 应用。`docs/PRODUCT.md` 是当前唯一有效产品需求；不得主动读取历史 PRD 作为当前需求来源。
# Tech Stack
Kotlin / Compose Material 3 / Navigation / ViewModel / Flow / Room / DataStore；纯 JVM domain 模块。
# Important Business Rules
系统时钟是当前时刻唯一事实来源；午休启用时不计薪，默认 09:30—19:00、12:30—14:00 午休、有效 8 小时；法定调休覆盖普通周规则；金额用 BigDecimal；不保存累计金额；核心计算完全离线。
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

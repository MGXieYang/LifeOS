# 离线日历
中国大陆存在周末补班，不能只看星期。assets/calendar/<year>.json 按 year、calendarVersion、calendarLastUpdated、source、days 组织，days 值仅允许 WORKDAY/HOLIDAY。
已覆盖 2025、2026；年度文件缺失时警告并按普通周估算，文件损坏应显式报错，不静默覆盖。
来源：
- 2025 国务院办公厅 https://www.gov.cn/zhengce/zhengceku/202411/content_6986383.htm
- 2026 国务院办公厅 https://www.gov.cn/zhengce/zhengceku/202511/content_7047091.htm
- 2026 核对页面 https://www.beijing.gov.cn/cs/gncs/zcwj/202603/t20260327_4568275.html
新增年份：等待官方公布 → 逐日录入所有假期和补班 → 检查跨年与节假日重叠 → 添加已知假期/补班单测 → 更新覆盖年份说明。App 运行期间不访问来源链接。
2026 年 9 月有 22 工作日：9 月 20 日补班，9 月 25—27 日中秋休假。

## 联网更新（用户批准的 PRD 修订）
设置页可按需更新系统当前年和下一年。使用 https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/{year}.json ，项目 https://github.com/NateScarlet/holiday-cn 。这是第三方开源整理数据，非官方 API；UI 明确标明来源。
只发送年份，不读取或上传个人设置。HTTPS、连接/读取超时各 10 秒、上限 256 KiB、禁重定向。校验年份、来源政府域名、日期范围、重复日期、布尔状态、七个主要节日覆盖。未发布/不完整/解析失败均保留旧数据，显示更新失败。
通过 AtomicFile 原子缓存完整响应和最近成功日期；下次启动加载缓存覆盖内置年份，版本为内容哈希。下一年度公告可能影响前一年 12 月，因此更新时附带尝试下一年；只将跨年日期覆盖到已有完整年度，不把几天数据误认成一整年。
没有运行时定时联网；用户点更新才请求。缓存持久化，不依赖网络可用性。

# LifeOS V3.0 状态

- [x] Phase 0：Gap Analysis 与 Room/DataStore 迁移计划。
- [x] Phase 1：LifeOS 品牌、Onboarding、当下首页、设置、四 Tab 导航和旧模块清理。
- [x] Phase 2：基础天平、真实天平视觉、Slider、工作代价与 PurchaseWeightPolicy。
- [x] Phase 3：长期使用价值、UsageValueCalculator 与用户主动保存的历史。
- [x] Phase 4：年度进度、人生时间轴、自定义节点和退休。
- [x] Phase 5：90/180/365 天未来时间预算。
- [x] 完成规定的 JVM/Android 单测、Debug APK 与 Lint 验证并记录实际结果。
- [ ] 真机或模拟器检查小屏、大字体、输入法、跟随系统/浅色/深色来回切换、页面返回和进程恢复。
- [ ] 在设备上运行 Room 1→2 instrumentation migration test，确认真实 V2 数据升级。

## 后续范围

V3 Phase 6 的插画深化、响应式视觉精修和新版数据导入/导出不在本轮 Phase 1→5 范围。实现前必须先补充与 V3 精简数据模型一致的格式要求，不得恢复 V2 Decision/FreeTime 备份结构。

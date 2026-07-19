# 个人轨迹

这是给 Operit 灵感系统配套的 Android 伴侣应用。它不使用 Shizuku、root 或 `dumpsys`。

## 第一次使用

1. 安装 `app-debug.apk`，打开应用。
2. 在系统页允许“使用情况访问”。
3. 选择 Operit 正在推送到 GitHub 的仓库根目录。应用会在其中写入 `YYYY-MM-DD/timeline.txt`、`notes.txt` 和 `screenshots/`。
4. 在 vivo 的电池设置中允许本应用自启动、后台运行和不受电池优化限制。

应用会用 WorkManager 约每小时运行一次。系统省电时可能延后，但下次会从上次采集点回查 Usage Events；它不会因延后而把中间的前台切换直接丢掉。

## 边界

- UsageStatsManager 给出的是系统记录的使用事件；“最近前台”是最新一条前台事件，而不是 `dumpsys` 式的实时进程查询。
- 记录碎碎念时可主动选择一张图片。应用只复制这张被选择的图片进入当天的 `screenshots/`，不会自动把最近截图或相册照片关联进笔记。
- 本应用不保存 GitHub Token，也不直接推送仓库；由 Operit/Git 同步流程推送所选目录。
- `timeline.txt` 是技术轨迹，灵感同步器不会把其中的通知、设备或应用轨迹写进个人画像或周报。

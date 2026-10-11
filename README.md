# 工程相机（Openbows Camera）

Android 竖屏水印相机：拍照时将时间、地址、坐标、海拔、天气、备注等现场信息**烧录进 JPEG**，便于巡检、验收与取证留档。

- 包名：`com.openbows.camera`
- 当前版本：`1.5.15`（versionCode 19）
- 系统要求：Android 8.0（API 26）及以上，仅竖屏
- 技术栈：Kotlin · Jetpack Compose · CameraX · Room · DataStore · OkHttp

---

## 1. 项目解决什么问题

现场拍照常需事后补写地点与时间，易出错、难核对。本应用在**按下快门时**把定位与模板字段直接画进照片像素，并保存到系统相册目录，形成可分享、可归档的取证底片。

---

## 2. 主要功能

| 模块 | 说明 |
|------|------|
| 拍照 | CameraX 预览与拍摄；前后摄切换、闪光灯；点击对焦、捏合变焦 |
| 实时水印 | 取景框叠加水印预览；拍照时烧录到 JPEG（非仅 EXIF） |
| 水印字段 | 经度、纬度、坐标系、地址、时间、海拔、天气、备注（可按模板开关） |
| 水印样式 | 字体（黑体 / 衬线 / 等宽）、字号（小 / 中 / 大）、文字色与背景色 |
| 水印模板 | 内置「现场巡检」「取证拍照」「施工验收」「安全检查」；支持新建、编辑、重命名、复制、删除与切换当前模板 |
| 定位与地址 | 系统 GPS（WGS84）；天地图逆地理获取地址（支持最多 5 个 tk 轮询） |
| 天气 | Open-Meteo 按经纬度查询（不消耗天地图配额） |
| 坐标系 | 全局可选 WGS84（推荐）、GCJ02、CGCS2000 |
| 备注 | 长按取景水印区域编辑；支持历史记录 |
| 相册 | 网格浏览、多选分享/删除；单张详情滑动查看与系统分享 |
| 关于 | 点击标题栏「Openbows Camera」滑出关于信息 |

照片目录：`Pictures/OpenbowsCamera`。

---

## 3. 安装方法（含项目结构）

### 项目结构

```text
Openbows-Camera/
├── README.md
├── build.gradle.kts              # 根工程与插件版本
├── settings.gradle.kts
├── gradle.properties
├── gradlew.bat
├── local.properties.example      # 配置模板（复制为 local.properties）
├── gradle/wrapper/               # Gradle Wrapper
├── keystore/                     # 本地签名（不入库）
├── dist/                         # 本地打包 APK（不入库）
└── app/
    ├── build.gradle.kts          # 应用模块、版本号、天地图 BuildConfig
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/                  # 字符串、主题、图标、FileProvider 路径
        └── java/com/openbows/camera/
            ├── MainActivity.kt
            ├── OpenbowsApplication.kt
            ├── data/             # Room、模板/相册仓库、偏好设置
            ├── location/         # 系统定位、天地图逆地理、坐标转换、天气
            ├── watermark/        # 水印排版与烧录
            └── ui/
                ├── OpenbowsRoot.kt
                ├── camera/       # 拍照页
                ├── album/        # 相册与详情
                ├── template/     # 模板列表与编辑
                ├── permission/   # 权限门禁
                └── theme/
```

### 环境

- Android Studio / JDK 17
- Android SDK（`compileSdk` / `targetSdk` 35）

### 配置

1. 复制 `local.properties.example` 为 `local.properties`，填写 `sdk.dir`。
2. （可选）填写天地图密钥，用于地址逆地理；不填则仍可拍照，地址可能为空：

```properties
TIANDITU_TK_1=你的密钥1
TIANDITU_TK_2=你的密钥2
TIANDITU_TK_3=
TIANDITU_TK_4=
TIANDITU_TK_5=
```

3. （可选）配置 release 签名：

```properties
RELEASE_STORE_FILE=keystore/openbows-release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=openbows_release
RELEASE_KEY_PASSWORD=...
```

### 编译安装

```bat
gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Release：

```bat
gradlew.bat assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```

也可直接安装本地已打好的包（若存在）：`dist\OpenbowsCamera-{版本}-debug.apk` / `...-release.apk`。  
**注意：** debug 与 release 签名不同，不能互相覆盖安装，需先卸载再装。

---

## 4. 使用方法

1. **首次启动**  
   系统询问相机、定位等权限；拒绝则退出应用。

2. **拍照页（默认）**  
   - 等待「定位已就绪」；地址/天气联网后自动填充  
   - 长按水印区域 → 编辑备注  
   - 点击预览对焦；底部快门拍照  
   - 左下角缩略图 → 相册；右下角「水印」→ 模板列表  
   - 点击标题「Openbows Camera」→ 关于  

3. **水印模板**  
   - 选择坐标系与当前模板  
   - 编辑模板：开关字段、备注、字体字号与颜色  

4. **相册**  
   - 浏览全部现场照片；多选后系统分享或删除  
   - 点进单张可左右滑动，分享或删除  

---

## 5. 输入输出示例

### 输入（系统 + 用户）

| 输入 | 来源 |
|------|------|
| 经纬度 / 海拔 | 系统定位 |
| 地址 | 天地图逆地理 |
| 天气 | Open-Meteo |
| 坐标系显示 | 模板页选择（如 WGS84） |
| 备注与字段开关、样式 | 水印模板 / 长按水印编辑 |
| 画面 | 相机快门 |

### 输出（烧录后的 JPEG）

- 路径：`Pictures/OpenbowsCamera/`  
- 文件名示例：`IMG_20261011_085000.jpg`，或带备注后缀：`IMG_20261011_085000_基坑验收第3段.jpg`（备注经清洗后取末尾最多 40 字）  
- 水印内容示例（字段全开时）：

```text
经度 116.391278
纬度 39.907431
坐标系 WGS84
地址 北京市东城区示例路88号
时间 2026-10-11 08:50:00
海拔 45.2m
天气 晴 26℃
备注：基坑验收第3段
```

空值仍保留字段名（如 `地址 `）。水印为左下角半透明底框，像素已写入图片，用任意看图软件打开即可核对。

---

## 许可证与联系

- 许可证：木兰宽松许可证，第2版（Mulan PSL v2），详见根目录 `LICENSE`  
- 版权：Copyright (c) 2026 Sun, Jingwen (http://www.openbows.com)  
- 开发者：孙景文 (Sun, Jingwen)  
- 邮箱：63451373@qq.com  

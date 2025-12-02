pluginManagement {
    repositories {
        // 调整仓库顺序，优先使用Google官方仓库
        google()
        gradlePluginPortal()
        mavenCentral()
        
        // 国内镜像源作为补充
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin/")
        maven(url = "https://maven.aliyun.com/repository/google/")
        maven(url = "https://maven.aliyun.com/repository/public/")

        // 添加额外的镜像源以增加成功率
        maven(url = "https://maven.aliyun.com/repository/jcenter/")
        maven(url = "https://mirrors.tencent.com/nexus/repository/maven-public/")
        
        // 添加KSP插件仓库
        maven(url = "https://plugins.gradle.org/m2/")
    }
}

// 添加buildscript配置来确保Android Gradle Plugin能正确加载
buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.aliyun.com/repository/google/")
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin/")
        maven(url = "https://maven.aliyun.com/repository/jcenter/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内镜像源优先
        maven(url = "https://maven.aliyun.com/repository/google/")
        maven(url = "https://maven.aliyun.com/repository/public/")
        maven(url = "https://maven.aliyun.com/repository/jcenter/")
        
        // 原始仓库作为后备
        google()
        mavenCentral()
        maven(url = "https://mirrors.tencent.com/nexus/repository/maven-public/")
    }
}

rootProject.name = "LoveDiary"
include(":app")
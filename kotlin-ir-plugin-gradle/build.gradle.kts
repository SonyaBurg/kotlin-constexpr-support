plugins {
  id("java-gradle-plugin")
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("com.gradle.plugin-publish")
}

apply(plugin = "maven-publish")
apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "org.jetbrains.kotlin.kapt")

dependencies {
  implementation(kotlin("gradle-plugin-api"))
}

buildConfig {
  val project = project(":kotlin-ir-plugin")
  packageName(project.group.toString())
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${project.group}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${project.name}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${project.version}\"")
}

gradlePlugin {
  plugins {
    create("kotlinIrPluginTemplate") {
      id = rootProject.extra["kotlin_plugin_id"] as String
      displayName = "Kotlin Ir Plugin Constant Evaluation"
      description = "Kotlin Ir Plugin Constant Evaluation"
      implementationClass = "com.redisco.constexpr.TemplateGradlePlugin"
    }
  }
}

publishing {
  publications {
    create("pluginMaven", MavenPublication::class) {
    }
  }
}

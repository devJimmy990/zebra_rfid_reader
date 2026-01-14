allprojects {
    repositories {
        google()
        mavenCentral()
        // Add plugin's local Maven repo here instead
        maven {
            url = uri("https://raw.githubusercontent.com/devJimmy990/rfid_zebra_reader/main/android/maven")
        }

    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}

subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
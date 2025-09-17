import com.github.spotbugs.snom.SpotBugsTask

plugins {
    java
    application
    jacoco
    checkstyle
    id("com.github.spotbugs") version "6.4.2"
    id("info.solidsoft.pitest") version "1.19.0-rc.1"
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "com.benjaminsproule"
version = rootProject.file("version.txt").readText().trim()

dependencies {
    compileOnly(libs.spotbugs.annotations)
    implementation(libs.commons.cli)
    implementation(libs.digital.blasphemy.client)
    testCompileOnly(libs.spotbugs.annotations)
    testImplementation(libs.assertj)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.12.1")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                minimum = "1.00".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "LINE"
                minimum = "1.00".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                minimum = "1.00".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "COMPLEXITY"
                minimum = "1.00".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "METHOD"
                minimum = "1.00".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "CLASS"
                minimum = "1.00".toBigDecimal()
            }
        }
    }
}

checkstyle {
    config =
        project.resources.text.fromUri("https://raw.githubusercontent.com/gigaSproule/checkstyle-config/refs/heads/main/checkstyle.xml")
}

spotbugs {
    excludeFilter = file("config/spotbugs/exclude.xml")
}

tasks.withType<SpotBugsTask> {
    reports {
        create("html") {
            enabled = true
            required = true
        }
    }
}

pitest {
    junit5PluginVersion = "1.2.3"
    mutationThreshold = 100
    coverageThreshold = 100
}

tasks.check {
    finalizedBy(tasks.jacocoTestCoverageVerification, tasks.pitest)
}

application {
    mainClass = "com.benjaminsproule.Main"
}

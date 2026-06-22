plugins {
    `java-library`
}

dependencies {
    api(project(":we-adk-welfare-common"))
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
}

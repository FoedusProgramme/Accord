plugins {
	id("java-library")
}

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
	compileOnly(project(":misc:audiofxstub"))
}

// Compile against the small official API surface without including those classes in the MPE.
val compileOfficialApi by tasks.registering(JavaCompile::class) {
    source(rootProject.fileTree("official-api") { include("**/*.java") })
    classpath = files()
    destinationDirectory.set(layout.buildDirectory.dir("official-api/classes"))
    options.release.set(8)
}
val officialApiJar by tasks.registering(Jar::class) {
    archiveFileName.set("official-api.jar")
    destinationDirectory.set(layout.buildDirectory.dir("official-api"))
    from(compileOfficialApi.flatMap { it.destinationDirectory })
}
dependencies { add("compileOnly", files(officialApiJar)) }

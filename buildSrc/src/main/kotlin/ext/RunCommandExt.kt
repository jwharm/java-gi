package ext

import org.gradle.api.Project
import java.io.ByteArrayOutputStream

fun String.runCommand(project: Project): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = this@runCommand.split("\\s".toRegex())
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

package ext

import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import org.apache.tools.ant.taskdefs.condition.Os

fun String.runCommand(project: Project, fallback: String): String {
    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        return fallback
    }
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = this@runCommand.split("\\s".toRegex())
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

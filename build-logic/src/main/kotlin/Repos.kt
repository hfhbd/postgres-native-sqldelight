import org.gradle.api.*

class Repos: Plugin<Project> {
    override fun apply(project: Project) {
        project.repositories.apply {
            repos()
        }
    }
}

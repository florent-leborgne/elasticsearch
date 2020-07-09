import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

version = "2020.1"

val developmentBranches = listOf("master", "7.x", "7.8", "6.8")

project {
    vcsRoot(GitVcsRoot {
        name = "Elasticsearch Kotlin DSL"

        id(name)
        url = "https://github.com/elastic/elasticsearch.git"
        branch = "refs/heads/teamcity"
    })

    developmentBranches.forEach { devBranch ->
        subProject {
            id(devBranch)
            name = devBranch

            vcsRoot(GitVcsRoot {
                name = "Elasticsearch ($devBranch)"

                id(name)
                url = "https://github.com/elastic/elasticsearch.git"
                branch = "refs/heads/$devBranch"
            })
        }
    }
}


# For developers of this SDK

## Releasing

A release is created through the following steps:

1. Update library versions (release task will fail if versions doesn't match the pushed tag - ignoring `v` prefix)
    1. `core/build.gradle`
    2. `sdk/build.gradle`
2. Push a version tag in the format `vX.Y.Z`; e.g. `v0.1.4`.
3. Once the `Release` workflow has successfully completed:
    1. Complete the GH draft release
    2. Promote the staged libraries at [OSSRH](https://s01.oss.sonatype.org) to Maven Central by
        1. `Closing` the staged repository, and then
        2. Test the staged libraries 
       ```
       repositories {
          mavenCentral()
          maven {
             url "https://s01.oss.sonatype.org/content/repositories/staging/"
          }
       }
       ```
        3. `Release` it (grab a coffee, it might take hours before the release shows up at Maven Central)

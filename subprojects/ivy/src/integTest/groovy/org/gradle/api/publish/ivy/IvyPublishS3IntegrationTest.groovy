/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.publish.ivy

import org.gradle.internal.resource.transport.aws.s3.S3ConnectionProperties
import org.gradle.test.fixtures.server.s3.S3FileBackedServer

class IvyPublishS3IntegrationTest extends AbstractIvyPublishIntegTest {

    String bucket = 'tests3bucket'

    public S3FileBackedServer server

    def setup() {
        server = new S3FileBackedServer(file())
        executer.withArgument("-D${S3ConnectionProperties.S3_ENDPOINT_PROPERTY}=${server.getUri()}")
    }

    def "can publish to a S3 Ivy repository"() {
        given:
        settingsFile << 'rootProject.name = "publishS3Test"'
        buildFile << """
apply plugin: 'java'
apply plugin: 'ivy-publish'

group = 'org.gradle.test'
version = '1.0'

publishing {
    repositories {
        ivy {
            url "s3://${bucket}"
            credentials(AwsCredentials) {
                accessKey "someKey"
                secretKey "someSecret"
            }
        }
    }
    publications {
        ivy(IvyPublication) {
            from components.java
        }
    }
}
"""

        when:
        succeeds 'publish'

        then:
        def ivyRepo = ivy(server.getBackingDir(bucket))
        def module = ivyRepo.module('org.gradle.test', 'publishS3Test', '1.0')
        module.assertPublishedAsJavaModule()
        // TODO Verify published checksums: move functionality from HttpArtifact to something that works for IvyFileRepository

    }
}

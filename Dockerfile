FROM maven:3-adoptopenjdk-8 as builder
MAINTAINER cbuchart@auchan.fr
COPY . /build
WORKDIR /build
RUN mvn clean package

FROM sonatype/nexus3:latest
ARG APPVERSION
USER root
RUN mkdir -p /opt/sonatype/nexus/system/fr/auchan/nexus3-gitlabauth-plugin/${APPVERSION}/
COPY --from=builder /build/target/nexus3-gitlabauth-plugin-${APPVERSION}.jar /opt/sonatype/nexus/system/fr/auchan/nexus3-gitlabauth-plugin/${APPVERSION}/
COPY --from=builder /build/target/feature/feature.xml /opt/sonatype/nexus/system/fr/auchan/nexus3-gitlabauth-plugin/${APPVERSION}/nexus3-gitlabauth-plugin-${APPVERSION}-features.xml
COPY --from=builder /build/pom.xml /opt/sonatype/nexus/system/fr/auchan/nexus3-gitlabauth-plugin/${APPVERSION}/nexus3-gitlabauth-plugin-${APPVERSION}.pom
RUN echo '<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>fr.auchan</groupId><artifactId>nexus3-gitlabauth-plugin</artifactId><versioning><release>${APPVERSION}</release><versions><version>${APPVERSION}</version></versions><lastUpdated>20170630132608</lastUpdated></versioning></metadata>' > /opt/sonatype/nexus/system/fr/auchan/nexus3-gitlabauth-plugin/maven-metadata-local.xml
RUN echo "mvn\:fr.auchan/nexus3-gitlabauth-plugin/${APPVERSION} = 200" >> /opt/sonatype/nexus/etc/karaf/startup.properties

USER nexus
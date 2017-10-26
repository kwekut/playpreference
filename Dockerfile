# Pull base image
FROM  openjdk:8u131-jdk

ENV SBT_VERSION 0.13.15
ENV CHECKSUM 18b106d09b2874f2a538c6e1f6b20c565885b2a8051428bd6d630fb92c1c0f96
ENV LIBSODIUM_VERSION 1.0.12
ENV PROJECT_HOME /usr/src


# Install some tools: gcc build tools, unzip, etc
RUN \
    apt-get update && \
    apt-get -y upgrade && \
    apt-get install -y apt-transport-https && \
    apt-get -y install curl build-essential unzip locate

# Download and install libsodium - https://download.libsodium.org/doc/
# Download & extract & make libsodium - Move libsodium build
RUN \
    mkdir -p /tmpbuild/libsodium && \
    cd /tmpbuild/libsodium && \
    curl -L https://download.libsodium.org/libsodium/releases/libsodium-${LIBSODIUM_VERSION}.tar.gz -o libsodium-${LIBSODIUM_VERSION}.tar.gz && \
    tar xfvz libsodium-${LIBSODIUM_VERSION}.tar.gz && \
    cd /tmpbuild/libsodium/libsodium-${LIBSODIUM_VERSION}/ && \
    ./configure && \
    make && make check && \
    make install && \
    mv src/libsodium /usr/local/ && \
    rm -Rf /tmpbuild/


# Install sbt
RUN \
echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && \
apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
apt-get update && \
apt-get install sbt

COPY . $PROJECT_HOME/app
WORKDIR $PROJECT_HOME/app

#Production
#RUN sbt dist 
#RUN unzip $PROJECT_HOME/app/target/universal/pref-1.0-SNAPSHOT.zip
#ENV PATH $PROJECT_HOME/app/target/universal/pref-1.0-SNAPSHOT/pref/bin:$PATH

#Production
RUN sbt clean stage
ENV PATH $PROJECT_HOME/app/target/universal/stage/bin:$PATH
 
#Healthcheck
HEALTHCHECK --interval=5m --timeout=3s \
  CMD curl -f http://localhost/health || exit 1

# Define default command
EXPOSE 9000
ENTRYPOINT ["pref", "-Dplay.crypto.secret=abcdefghijk"]
CMD ["-d"]

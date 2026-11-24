FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /build
COPY build.sh ./
COPY src/ src/
RUN sh build.sh

FROM eclipse-temurin:21-jre-noble
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        fonts-dejavu-core libxi6 libxrender1 libxtst6 novnc openbox x11-utils x11vnc xvfb \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 gimnasio \
    && install -d -o gimnasio -g gimnasio /data

WORKDIR /app
COPY --from=build /build/build/gimnasio.jar ./
COPY docker/start.sh ./
ENV DISPLAY=:99 GYM_DATA_DIR=/data
USER gimnasio
EXPOSE 6080
ENTRYPOINT ["/bin/bash", "/app/start.sh"]

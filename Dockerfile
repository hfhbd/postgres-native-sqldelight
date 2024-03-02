# Define project root inside build container
ARG SOURCE_ROOT=/usr/src/postgres-native-sqldelight-driver

# Define builder distro:
#   "ubuntu" (ubuntu:noble, default)
#   "alpine" (alpine:3)
ARG BUILDER_DISTRO=ubuntu

# Define runtime distro:
#   "ubuntu" (ubuntu:noble, default)
#   "alpine" (alpine:3)
ARG RUNTIME_DISTRO=ubuntu

################################################################################
# STAGE 1: setup builders
################################################################################

#  Kotlin/Native prebuilt compiler is available for x86_64 Linux only, so Use "linux/amd64" platform for base builder image
FROM --platform=linux/amd64 ubuntu:noble AS ubuntu-builder-base
RUN \
    --mount=type=cache,sharing=locked,target=/var/lib/apt/lists \
    --mount=type=cache,sharing=locked,target=/var/cache/apt \
    apt-get update && \
    apt install -y \
    git \
    cmake \
    pkgconf \
    build-essential \
    crossbuild-essential-arm64 \
    openjdk-17-jdk-headless

FROM ubuntu-builder-base AS ubuntu-builder-base-amd64
RUN \
    --mount=type=cache,sharing=locked,target=/var/lib/apt/lists \
    --mount=type=cache,sharing=locked,target=/var/cache/apt \
    apt-get install --no-install-recommends -y libpq-dev:amd64

FROM ubuntu-builder-base AS ubuntu-builder-base-arm64
ADD ubuntu-arm64-sources.list /etc/apt/sources.list.d/
RUN \
    --mount=type=cache,sharing=locked,target=/var/lib/apt/lists \
    --mount=type=cache,sharing=locked,target=/var/cache/apt \
    apt-get update && \
    dpkg --add-architecture arm64 && \
    apt-get install --no-install-recommends -y libpq-dev:arm64

FROM ubuntu-builder-base-${TARGETARCH} AS ubuntu-builder-base

FROM --platform=linux/amd64 alpine:3 AS alpine-builder-base-amd64
# TODO
RUN echo "Not implemented: dependencies for building under alpine should be resolved, use ubuntu builder instead" && exit 1
RUN apk update && apk add --no-cache libstdc++ libpq-dev openjdk17-jdk

FROM alpine-builder-base-${TARGETARCH} AS alpine-builder-base

FROM ${BUILDER_DISTRO}-builder-base AS builder

################################################################################
# STAGE 2: add project sources and configure gradle build
################################################################################

FROM builder AS sources
ARG SOURCE_ROOT
WORKDIR ${SOURCE_ROOT}
ADD . .
RUN chmod +x ./gradlew

FROM sources AS gradlew
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
RUN \
    --mount=type=cache,sharing=private,target=/root/.konan \
    --mount=type=cache,sharing=private,target=/root/.gradle \
    ./gradlew

################################################################################
# STAGE 3: compile binaries
################################################################################

FROM gradlew AS test-binaries-amd64
ARG SOURCE_ROOT
RUN ./gradlew :testing:linuxX64TestBinaries --info --stacktrace

FROM gradlew AS test-binaries-arm64
ARG SOURCE_ROOT
RUN ./gradlew :testing:linuxArm64TestBinaries --info --stacktrace

FROM test-binaries-${TARGETARCH} AS test-binaries

################################################################################
# STAGE 4: copy binaries & prepare runtime
################################################################################

# This target doesn't use "--platform" argument, so the target image will be multiarch
FROM ubuntu:noble AS ubuntu-test-runtime-base
RUN apt-get update && apt-get install --no-install-recommends -y libpq-dev file

FROM alpine:3 AS alpine-test-runtime-base
RUN \
    apk update && \
    apk add --no-cache \
    gcompat g++ libpq-dev file

FROM ${RUNTIME_DISTRO}-test-runtime-base AS test-runtime-base
WORKDIR /usr/local/bin

FROM --platform=linux/amd64 test-runtime-base AS test-runtime-amd64
ENV KOTLIN_NATIVE_TARGET=linuxX64

FROM --platform=linux/arm64 test-runtime-base AS test-runtime-arm64
ENV KOTLIN_NATIVE_TARGET=linuxArm64

FROM test-runtime-${TARGETARCH} AS test-runtime
ARG SOURCE_ROOT
COPY --from=test-binaries ${SOURCE_ROOT}/testing/build/bin/${KOTLIN_NATIVE_TARGET}/debugTest/test.kexe test.kexe
ENTRYPOINT [ "sh", "-c" ]
CMD [ "./test.kexe" ]

# Default target
FROM test-runtime

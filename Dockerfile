FROM archlinux:base-devel

COPY pacman.conf /etc/pacman.conf
COPY mirrorlist /etc/pacman.d/mirrorlist
RUN pacman -Syu --noconfirm jdk-openjdk

COPY target/scala-3.*/autopkg-assembly-0.1.0-SNAPSHOT.jar /usr/lib/autopkg.jar

ENV AUTOPKG_DB_URI=jdbc:postgresql://postgres/autopkg
ENV AUTOPKG_DB_USER=autopkg
ENV AUTOPKG_DB_PASSWORD=autopkg
ENV AUTOPKG_REPO_DIR=/repo
ENV AUTOPKG_TMP_DIR=/tmp/autopkg
ENTRYPOINT java -jar /usr/lib/autopkg.jar build
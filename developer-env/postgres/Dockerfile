FROM postgres:10-alpine
ENV POSTGRES_USER docker
ENV POSTGRES_PASSWORD docker
ENV POSTGRES_DB docker
ADD rootfs/init.sql /docker-entrypoint-initdb.d/
#!/bin/bash

DOCKER_IMAGE_NAME=thunder-api-image
CONTAINER_NAME=thunder-api-container

cleanup_container() {
  local container_name=$1
  local running_containers=$(docker ps -a --filter "name=${container_name}" --format "{{.ID}}")

  if [[ -n "${running_containers}" ]]; then
    echo "Stopping and removing existing container ${container_name}..."
    docker stop ${running_containers} > /dev/null
    docker rm ${running_containers} > /dev/null
    echo "Removed existing container ${container_name}."
  fi
}
cleanup_container ${CONTAINER_NAME}

docker rmi ${DOCKER_IMAGE_NAME}
docker build --force-rm \
  --platform=linux/amd64 \
  -t ${DOCKER_IMAGE_NAME} .
docker run -d \
  -p 9000:9000 \
  --name ${CONTAINER_NAME} \
  ${DOCKER_IMAGE_NAME}

docker builder prune -f

echo "Successfully started ${CONTAINER_NAME}"
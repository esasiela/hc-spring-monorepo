#!/bin/bash

# Constants
HC_AUTH_DIR="hc-auth-api"
DOCKER_IMAGE="hc-auth-api"
CONTAINER_NAME="hc-auth-api"
HOST_PORT=8081
CONTAINER_PORT=8080
PROFILE="dev"

# Verify HC_AUTH_INIT_PASSWORD is set
if [ -z "$HC_AUTH_INIT_PASSWORD" ]; then
  echo "Error: must set HC_AUTH_INIT_PASSWORD"
  exit 1
fi

# Check if hc-auth-api directory exists
if [ ! -d "$HC_AUTH_DIR" ]; then
  echo "Error: must run from monorepo root"
  exit 1
fi

# Parse command-line arguments for build option
BUILD=false
for arg in "$@"; do
  case $arg in
    -b|--build)
      BUILD=true
      shift
      ;;
    *)
      ;;
  esac
done

if $BUILD; then
  echo "Building maven project..."
  mvn clean install
  if [ $? -ne 0 ]; then
    echo "Error: maven build failed"
    exit 1
  fi
fi

# Change to hc-auth-api directory
cd "$HC_AUTH_DIR" || exit

# Run docker build if -b or --build is specified
if $BUILD; then
  echo "Building Docker image..."
  docker build -t "$DOCKER_IMAGE" .
  if [ $? -ne 0 ]; then
    echo "Error: Docker build failed"
    exit 1
  fi
fi

# Check for existing container
if [ "$(docker ps -a --no-trunc --filter name=^$CONTAINER_NAME$ | wc -l)" -eq 2 ]; then
  echo "Stopping and removing existing container '$CONTAINER_NAME'..."
  docker stop $CONTAINER_NAME && docker rm $CONTAINER_NAME
else
  echo "No container found with name '$CONTAINER_NAME'."
fi

# Run docker container
echo "Running Docker container..."
docker run -d \
  --name "$CONTAINER_NAME" \
  -p "$HOST_PORT:$CONTAINER_PORT" \
  -e "SPRING_PROFILES_ACTIVE=$PROFILE" \
  -e "HC_AUTH_INIT_PASSWORD=$HC_AUTH_INIT_PASSWORD" \
  "$DOCKER_IMAGE"
if [ $? -ne 0 ]; then
  echo "Error: Docker run failed"
  exit 1
fi

echo "Docker container $CONTAINER_NAME is running."

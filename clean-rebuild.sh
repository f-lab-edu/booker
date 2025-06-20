#!/bin/bash

# Stop all containers and remove volumes
echo "Stopping containers and removing volumes..."
docker compose down -v

# Remove project related images
echo "Removing Docker images..."
docker rmi booker-springboot:latest


# Remove dangling images
echo "Removing dangling images..."
docker image prune -f

# Rebuild and start services
echo "Rebuilding and starting services..."
docker compose up --build -d

echo "Clean-up and rebuild completed!" 
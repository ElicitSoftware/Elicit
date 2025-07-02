# Build the multi-arch image
docker buildx build --platform linux/amd64,linux/arm64 --no-cache -t elicitsoftware/elicit_db:latest --load .

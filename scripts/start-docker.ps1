param(
  [switch]$Build
)

if ($Build) {
  docker compose up --build -d
} else {
  docker compose up -d
}

docker compose ps

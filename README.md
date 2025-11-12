# Das verrückte Labyrinth - Server

![Build Status](https://github.com/AIP-Labyrinth-Group3/labyrinth-server/workflows/Server%20CI/badge.svg)

Game server for "Das verrückte Labyrinth" - MCI AIP Project WS 2025/26

## Status

🚧 **In Development** - CI/CD Infrastructure setup complete, application code will be added incrementally.

## Setup

### Prerequisites
- JDK 17+
- Maven 3.8+

### Build
```bash
mvn clean compile
```

## CI/CD

- **CI Pipeline**: Validates and compiles on every push/PR to `main` and `develop`
- **CD Pipeline**: Runs on push to `main`

## Project Structure
```
labyrinth-server/
├── .github/workflows/    # CI/CD pipelines
├── src/                  # Source code (to be added)
├── pom.xml              # Maven configuration
└── README.md
```

## Development Workflow

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines.

## Codestyle

See [CODESTYLE.md](CODESTYLE.md) for codestyle guidelines.

## Team

Gruppe 3 - Clemens Siebers, Rene Stockinger, Andreas Rofner, Mario Gottwald, Simon Raass, Manuel Kirchebner, David Strauß

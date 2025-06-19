# Keycloak Hostname Configuration Strategy

## Overview
This document outlines the flexible hostname configuration approach for our Keycloak and Spring Boot integration.

## Configuration Principles
- Dynamic URL resolution
- Environment-specific overrides
- Flexible network context support

## Environment Variables

### Keycloak Hostname Configuration
- `KC_HOSTNAME`: Primary hostname (default: localhost)
- `KC_HOSTNAME_ADMIN`: Admin console hostname (default: localhost)
- `KC_HOSTNAME_PORT`: Service port (default: 8083)
- `KC_HOSTNAME_STRICT`: Hostname strict verification (default: false)
- `KC_HOSTNAME_STRICT_HTTPS`: HTTPS strict verification (default: false)
- `KC_HOSTNAME_STRICT_BACKCHANNEL`: Backchannel strict verification (default: false)

### URL and Redirect Configuration
- `KEYCLOAK_EXTERNAL_URL`: External Keycloak URL (default: http://localhost:8083)
- `KEYCLOAK_INTERNAL_URL`: Internal Keycloak URL (default: http://keycloak:8083)
- `KEYCLOAK_ALLOWED_REDIRECT_URIS`: Comma-separated list of allowed redirect URIs
- `KEYCLOAK_WEB_ORIGINS`: Comma-separated list of allowed web origins

## Logging Configuration
- `KC_LOG_LEVEL`: Logging verbosity (default: INFO)
- `KC_LOG_CONSOLE_COLOR`: Colored console output (default: true)
- `KC_LOG_CONSOLE_OUTPUT`: Console output format (default: default)

## Best Practices
1. Use environment variables for configuration
2. Maintain separate configurations for development and production
3. Always test authentication flows in different network contexts

## Example .env File
```
KC_HOSTNAME=mycompany.com
KC_HOSTNAME_PORT=443
KEYCLOAK_EXTERNAL_URL=https://auth.mycompany.com
KEYCLOAK_ALLOWED_REDIRECT_URIS=https://app1.mycompany.com,https://app2.mycompany.com
KC_LOG_LEVEL=DEBUG
```

## Troubleshooting
- Verify redirect URIs match your application's actual URLs
- Check network configuration in containerized environments
- Use DEBUG logging for detailed hostname resolution information
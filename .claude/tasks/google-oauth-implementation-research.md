# Google OAuth Implementation Research - 2025 Best Practices

**Research Date:** December 16, 2025
**Target Stack:** Next.js + Spring Boot + Keycloak
**Purpose:** Implementation plan for Google OAuth login with modern security practices

---

## Executive Summary

This research covers the latest 2025 best practices for implementing Google OAuth 2.0 authentication in a Next.js frontend with Spring Boot backend, using Keycloak as the OAuth/OIDC provider. The authentication landscape has evolved significantly with OAuth 2.1 making PKCE mandatory, Google deprecating the Sign-In JavaScript library in favor of Google Identity Services, and modern security practices emphasizing token rotation and secure storage.

---

## 1. Google OAuth 2.0 Flow - 2025 Standard

### Authorization Code Flow with PKCE (Mandatory)

**Key Changes in 2025:**
- **OAuth 2.1 makes PKCE mandatory** for ALL clients (not just public clients)
- Published January 2025 as Best Current Practice by IETF
- PKCE is now required even for confidential clients (traditional web apps)

**Why PKCE is Mandatory:**
- Protects against CSRF attacks
- Prevents authorization code injection attacks
- Provides "strong protection against misuse and injection of authorization codes"
- Mitigates authorization code interception in mobile/desktop apps

**Implementation Requirements:**
1. Generate cryptographically secure random code verifier (43-128 characters)
2. Create code challenge using SHA-256: `BASE64URL(SHA256(code_verifier))`
3. Send code challenge with authorization request
4. Send code verifier with token exchange request
5. Authorization server validates code_verifier matches original challenge

**Google-Specific Consideration:**
- Google's OAuth2 implementation **requires client_secret even when using PKCE** for Web Application client types
- This differs from RFC 7636 standard but is a Google implementation detail
- Mobile/native apps can use PKCE without client_secret

### State Parameter (CSRF Protection)

**Best Practice (2025):**
- Always use state parameter for CSRF protection in GET redirects
- If PKCE is confirmed supported by authorization server, it provides CSRF protection
- For OpenID Connect flows, nonce parameter also provides CSRF protection
- State parameter should be:
  - One-time use CSRF token
  - Cryptographically secure random value
  - Securely bound to user agent (session)
  - Validated on callback

**Implementation Pattern:**
```
1. Generate random state value → store in session
2. Include state in authorization URL
3. Validate state parameter in callback matches session
4. Consume state (delete from session) after validation
```

---

## 2. Google Identity Services vs Google Sign-In JavaScript Library

### Current Status (2025)

**Google Sign-In JavaScript Library:**
- **Deprecated:** March 31, 2023
- **Sunset date:** Not yet announced but imminent
- **New OAuth Client IDs:** Cannot use deprecated library
- **Migration:** Required for all applications

**Google Identity Services (GIS):**
- **Current standard:** Official replacement since 2023
- **Privacy-focused:** Migrating to Federated Credential Manager (FedCM) API
- **Modern architecture:** Addresses third-party cookie deprecation in Chrome

### Key Architectural Changes

#### Separation of Authentication and Authorization

**Old Approach (Deprecated):**
- Combined authentication and authorization in single flow
- Mixed ID tokens and access tokens
- OAuth 2.0 authorization codes, access tokens, and refresh tokens for both purposes

**New Approach (GIS):**
- **Authentication:** Sign-in using JWT ID tokens
  - Purpose: User identity verification
  - Components: Sign in with Google button, One Tap, automatic sign-in
  - Token: ID token (short-lived, contains user profile)

- **Authorization:** Separate API call for access tokens
  - Purpose: Access Google APIs or user data
  - Pattern: Incremental authorization
  - Token: Access token (for API calls)

### Benefits of Google Identity Services

**For Users:**
- One Tap login experience
- Automatic sign-in capability
- Refreshed, personalized button design
- Consistent branding across websites
- Better privacy controls (FedCM)

**For Developers:**
- Simplified integration (can use just HTML)
- Reduced complexity
- Automatic CSRF protection
- Lower technical barrier to entry
- Code generators available
- Follows incremental authorization best practice

### Migration Requirements

**If using authentication only (sign-in):**
- Follow: Google Identity Services authentication migration guide
- Replace: gapi.auth2 with Google Identity Services
- Change: Token handling from OAuth tokens to JWT ID tokens

**If using authorization (API access):**
- Follow: Google Identity Services authorization migration guide
- Separate: Authentication flow from authorization flow
- Update: API client library integration

**If using both:**
- Must follow BOTH migration guides
- Maintain clear separation between authentication and authorization

---

## 3. Spring Boot Integration Patterns

### Dependencies (2025)

**Required:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Optional (for cloud deployments):**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-oauth2</artifactId>
</dependency>
```

### Configuration Approaches

#### Option 1: Direct Google OAuth (Without Keycloak)

**Application Properties:**
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - openid
              - profile
              - email
        provider:
          google:
            issuer-uri: https://accounts.google.com
```

**Default Redirect URI:**
- Spring Boot auto-configures: `/login/oauth2/code/{registrationId}`
- For Google: `/login/oauth2/code/google`
- Must match Google Cloud Console configuration

**Security Configuration:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/dashboard")
                .failureUrl("/login?error")
                // Custom handlers
                .successHandler(customSuccessHandler())
                .failureHandler(customFailureHandler())
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/login").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

**Consent Screen Configuration:**
- Required scopes: `email`, `profile`, `openid`
- Google shares user profile after authentication
- Scopes determine data access permissions

#### Option 2: OAuth Resource Server (JWT Validation)

**For validating tokens from Keycloak or other providers:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/demo
          # OR direct JWKS endpoint:
          # jwk-set-uri: http://localhost:8080/realms/demo/protocol/openid-connect/certs
```

**How JWT Validation Works:**
1. Authorization server (Keycloak) issues signed JWT access tokens
2. Spring Boot validates tokens locally using public keys (JWKS)
3. No need to call Keycloak for each request
4. High performance, low coupling

**Validation Process:**
1. Query `.well-known/openid-configuration` for `jwks_uri`
2. Query `jwks_uri` for supported algorithms and public keys
3. Configure validation strategy
4. Validate each JWT's signature and `iss` claim

**Custom Role Mapping (Keycloak-specific):**
```java
@Configuration
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            new JwtIssuerValidator(issuerUri)
        ));
        return jwtDecoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter =
            new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            grantedAuthoritiesConverter
        );
        return jwtAuthenticationConverter;
    }
}
```

### Common Integration Challenges

**Next.js + Spring Boot Specific Issues:**

1. **OPTIONS Preflight Requests:**
   - Next.js sends OPTIONS before GET requests
   - Spring Boot's `BearerTokenAuthenticationFilter` may reject before CORS filter
   - Solution: Configure CORS properly and allow OPTIONS without authentication

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

2. **CSRF Token Management:**
   - May need to disable CSRF for stateless API
   - If using session-based auth, implement proper CSRF token handling
   - Consider using double-submit cookie pattern

---

## 4. Keycloak as OAuth/OIDC Provider with Google Identity Provider

### Architecture Overview

**Flow:**
```
User → Next.js → Keycloak → Google Identity Provider → Keycloak → Next.js → Spring Boot
```

**Benefits:**
- Centralized authentication management
- Multiple identity provider support (Google, GitHub, Facebook, etc.)
- Consistent token format across providers
- Role-based access control (RBAC)
- Single Sign-On (SSO) capability
- Token customization and mapping

### Keycloak Configuration

#### 1. Google Cloud Console Setup

**Steps:**
1. Create new project in Google Cloud Console
2. Navigate to APIs & Services → Credentials
3. Create OAuth 2.0 Client ID (Web application type)
4. Configure authorized redirect URI:
   ```
   https://YOUR_KEYCLOAK_DOMAIN/auth/realms/YOUR_REALM_NAME/broker/google/endpoint
   ```
5. Copy Client ID and Client Secret

**Important:**
- Redirect URI must exactly match Keycloak's format
- Use HTTPS in production
- Configure OAuth consent screen with required scopes

#### 2. Keycloak Identity Provider Setup

**Configuration Steps:**
1. Log into Keycloak admin console
2. Select realm
3. Navigate to Identity Providers
4. Click "Add provider" → Select "Google"
5. Enter:
   - **Client ID:** From Google Cloud Console
   - **Client Secret:** From Google Cloud Console
   - **Default Scopes:** `openid profile email` (required)
   - **Hosted Domain (optional):** Restrict to specific Google Workspace domain

**Default Scopes:**
```
openid profile email
```

**Optional Configuration:**
- **Hosted Domain (`hd` parameter):**
  - Restricts login to specific Google Workspace domain
  - Google validates domain in returned identity token
  - Format: `example.com`

- **Store Tokens:** Enable to store external tokens
- **Trust Email:** Auto-verify emails from Google
- **First Login Flow:** Configure user creation/update behavior

#### 3. Realm Configuration

**Token Settings:**
```
Access Token Lifespan: 15-30 minutes (recommended)
SSO Session Idle: 30 minutes
SSO Session Max: 10 hours
Refresh Token:
  - Rotation: Enabled (OAuth 2.1 best practice)
  - Reuse Interval: 0 (single-use tokens)
  - Max Lifespan: 7-14 days
```

**Client Configuration (for Next.js):**
```
Client ID: nextjs-app
Client Protocol: openid-connect
Access Type: public (for SPA) or confidential (for SSR)
Valid Redirect URIs: http://localhost:3000/api/auth/callback/keycloak
Web Origins: http://localhost:3000
```

**Client Configuration (for Spring Boot - Resource Server):**
```
Client ID: spring-backend
Client Protocol: openid-connect
Access Type: bearer-only (resource server)
```

### Terraform Configuration (Infrastructure as Code)

```hcl
resource "keycloak_oidc_google_identity_provider" "google" {
  realm             = "my-realm"
  client_id         = var.google_client_id
  client_secret     = var.google_client_secret
  hosted_domain     = "example.com"  # Optional
  default_scopes    = "openid profile email"

  trust_email       = true
  store_token       = false
  enabled           = true
}
```

### Token Flow with Keycloak

**Authentication Flow:**
1. User clicks "Sign in with Google" in Next.js app
2. Next.js redirects to Keycloak
3. Keycloak redirects to Google (Identity Provider)
4. User authenticates with Google
5. Google redirects back to Keycloak with authorization code
6. Keycloak exchanges code for Google tokens
7. Keycloak creates/updates user in its database
8. Keycloak issues its own tokens (access, refresh, ID)
9. Keycloak redirects to Next.js with Keycloak tokens
10. Next.js stores tokens and uses them for API calls to Spring Boot

**Token Types:**
- **Keycloak Access Token (JWT):** For API authorization
- **Keycloak Refresh Token:** For obtaining new access tokens
- **Keycloak ID Token (JWT):** For user identity information
- **Google Tokens (optional):** Stored if "Store Tokens" enabled

---

## 5. Security Best Practices (2025)

### PKCE Implementation

**Mandatory for All Clients (OAuth 2.1):**
- Public clients (SPAs, mobile, desktop): PKCE required
- Confidential clients (backend web apps): PKCE strongly recommended
- Google Web Application clients: Require both PKCE + client_secret

**Implementation:**
```javascript
// Frontend (Next.js)
const generateCodeVerifier = () => {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return base64URLEncode(array);
};

const generateCodeChallenge = async (verifier) => {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const hash = await crypto.subtle.digest('SHA-256', data);
  return base64URLEncode(new Uint8Array(hash));
};

// Store verifier in session storage (not local storage!)
const codeVerifier = generateCodeVerifier();
sessionStorage.setItem('pkce_code_verifier', codeVerifier);

const codeChallenge = await generateCodeChallenge(codeVerifier);
// Include in authorization URL: code_challenge & code_challenge_method=S256
```

### State Parameter Best Practices

**Generation:**
```javascript
const generateState = () => {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return base64URLEncode(array);
};

const state = generateState();
sessionStorage.setItem('oauth_state', state);
```

**Validation:**
```javascript
// On callback
const callbackState = new URLSearchParams(window.location.search).get('state');
const storedState = sessionStorage.getItem('oauth_state');

if (callbackState !== storedState) {
  throw new Error('CSRF attack detected: state mismatch');
}

// Consume state (delete after validation)
sessionStorage.removeItem('oauth_state');
```

### Token Storage (Platform-Specific)

#### Single-Page Applications (Next.js Client Components)

**DO NOT:**
- ❌ Store tokens in Local Storage (vulnerable to XSS)
- ❌ Store tokens in Session Storage (vulnerable to XSS)
- ❌ Store refresh tokens in browser (high risk)

**RECOMMENDED APPROACHES:**

**Option 1: In-Memory Storage (Most Secure)**
```javascript
// Store only in React state or closure
let accessToken = null;

const setToken = (token) => {
  accessToken = token;
};

// Forces re-authentication when token expires
// Best for high-security applications
```

**Option 2: Backend-for-Frontend (BFF) Pattern (Recommended)**
```
Architecture:
Next.js Frontend → Next.js API Routes (BFF) → Spring Boot Backend
                    ↓
                All tokens stored server-side
                HttpOnly, Secure, SameSite cookies for session
```

**BFF Implementation:**
```javascript
// pages/api/auth/[...nextauth].js
import NextAuth from "next-auth";
import KeycloakProvider from "next-auth/providers/keycloak";

export default NextAuth({
  providers: [
    KeycloakProvider({
      clientId: process.env.KEYCLOAK_CLIENT_ID,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET,
      issuer: process.env.KEYCLOAK_ISSUER,
    }),
  ],
  session: {
    strategy: "jwt", // or "database" for server-side sessions
  },
  callbacks: {
    async jwt({ token, account }) {
      // Store tokens server-side
      if (account) {
        token.accessToken = account.access_token;
        token.refreshToken = account.refresh_token;
        token.expiresAt = account.expires_at;
      }
      return token;
    },
    async session({ session, token }) {
      // Never send refresh token to client
      session.accessToken = token.accessToken;
      session.error = token.error;
      return session;
    },
  },
  cookies: {
    sessionToken: {
      name: `__Secure-next-auth.session-token`,
      options: {
        httpOnly: true,
        sameSite: 'lax',
        path: '/',
        secure: true, // HTTPS only in production
      },
    },
  },
});
```

#### Next.js Server Components / Server-Side Rendering

**Preferred Approach:**
```javascript
// app/api/protected/route.ts
import { getServerSession } from "next-auth/next";
import { authOptions } from "../auth/[...nextauth]";

export async function GET() {
  const session = await getServerSession(authOptions);

  if (!session) {
    return new Response("Unauthorized", { status: 401 });
  }

  // Use access token for backend API calls
  const response = await fetch('http://spring-boot-api/resource', {
    headers: {
      'Authorization': `Bearer ${session.accessToken}`,
    },
  });

  return response;
}
```

#### Mobile Applications (iOS/Android)

**Platform-Specific Secure Storage:**
- **iOS:** Keychain Services API
  ```swift
  // Example: Storing token in iOS Keychain
  let query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrAccount as String: "access_token",
      kSecValueData as String: tokenData,
      kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
  ]
  SecItemAdd(query as CFDictionary, nil)
  ```

- **Android:** Keystore System / EncryptedSharedPreferences
  ```kotlin
  // Example: EncryptedSharedPreferences
  val masterKey = MasterKey.Builder(context)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()

  val sharedPreferences = EncryptedSharedPreferences.create(
      context,
      "secure_prefs",
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
  )
  ```

#### Backend/Server Applications (Spring Boot)

**Refresh Token Storage:**
```java
// Encrypt tokens at rest in database
@Entity
public class UserToken {
    @Id
    private Long id;

    @Convert(converter = EncryptedStringConverter.class)
    private String refreshToken;

    private Instant expiresAt;
    private String tokenHash; // For revocation
}

// Database-level encryption + application encryption
// Restrict database access with IAM
// Rotate encryption keys regularly
```

### Credential Management

**DO:**
- ✅ Use secret managers (Google Cloud Secret Manager, AWS Secrets Manager, HashiCorp Vault)
- ✅ Rotate credentials regularly
- ✅ Use environment variables (never hardcode)
- ✅ Implement least-privilege access
- ✅ Audit credential usage

**DO NOT:**
- ❌ Hardcode credentials in source code
- ❌ Commit credentials to version control
- ❌ Store credentials in configuration files
- ❌ Share credentials across environments
- ❌ Use production credentials in development

**Example (.env.local):**
```env
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxxxxxx
KEYCLOAK_CLIENT_ID=nextjs-app
KEYCLOAK_CLIENT_SECRET=xxxxxxxxxxxx
KEYCLOAK_ISSUER=http://localhost:8080/realms/demo
NEXTAUTH_SECRET=generate-with-openssl-rand-base64-32
NEXTAUTH_URL=http://localhost:3000
```

**Secret Generation:**
```bash
# Generate NextAuth secret
openssl rand -base64 32

# Generate PKCE code verifier
openssl rand -base64 32 | tr -d "=+/" | cut -c1-43
```

### Scope Management (Incremental Authorization)

**Best Practice:**
- Request minimal scopes initially (openid, profile, email)
- Request additional scopes when functionality is needed
- Explain why each scope is needed in context
- Allow users to deny optional scopes
- Disable features gracefully if scopes denied

**Example:**
```javascript
// Initial authentication - minimal scopes
const basicScopes = ['openid', 'profile', 'email'];

// Later, when user wants to access Google Calendar
const requestCalendarAccess = async () => {
  const additionalScopes = ['https://www.googleapis.com/auth/calendar.readonly'];

  // Show explanation to user first
  if (await showScopeExplanation('calendar access')) {
    await requestAdditionalScope(additionalScopes);
  }
};
```

**Google Consent Screen:**
- Configure all possible scopes upfront
- Request only what's needed per session
- Scopes determine data access permissions
- Users see Google's permission dialog

### OAuth 2.1 Security Requirements (2025)

**Mandatory:**
1. **PKCE for all clients** (public and confidential)
2. **Exact redirect URI matching** (no substring matching)
3. **Refresh token rotation** (single-use tokens)
4. **Short-lived access tokens** (15-30 minutes recommended)

**Deprecated/Removed:**
- ❌ Implicit flow (use Authorization Code + PKCE instead)
- ❌ Resource Owner Password Credentials flow
- ❌ Fuzzy redirect URI matching
- ❌ Long-lived access tokens

**New Best Practices:**
- ✅ State parameter for all flows (CSRF protection)
- ✅ Token binding (DPoP - Demonstrating Proof-of-Possession)
- ✅ Pushed Authorization Requests (PAR)
- ✅ JWT access tokens with short expiration

---

## 6. Token Refresh Strategies (2025)

### Refresh Token Fundamentals

**Purpose:**
- Enable short access token lifetimes without constant re-authentication
- Maintain user session across access token expirations
- Balance security and user experience

**How It Works:**
1. User authenticates → receives access token (15-30 min) + refresh token (7-14 days)
2. Access token expires
3. Client sends refresh token to authorization server
4. Server validates refresh token
5. Server issues new access token (+ optionally new refresh token)
6. User continues without re-authentication

### Refresh Token Rotation (OAuth 2.1 Requirement)

**What Is Token Rotation:**
- Refresh token is replaced after each use
- Old refresh token becomes invalid
- New refresh token issued with each refresh
- Single-use refresh tokens

**Why It's Required:**
- Mitigates refresh token theft
- Limits damage from compromised tokens
- Enables replay attack detection
- Aligns with zero-trust security model

**Configuration:**
```
Rotation Type: ROTATE (not STATIC)
Reuse Detection: Enabled
Grace Period: 0 seconds (strict) or 30-60 seconds (lenient)
```

**Keycloak Configuration:**
```
Realm → Tokens → Revoke Refresh Token: Enabled
Realm → Tokens → Refresh Token Max Reuse: 0
```

### Token Lifetime Recommendations (2025)

**Access Tokens:**
- **Lifespan:** 15-30 minutes
- **Format:** JWT (self-contained)
- **Storage:** In-memory (SPA) or HttpOnly cookie (BFF)
- **Validation:** Local (JWT signature verification)

**Refresh Tokens:**
- **Lifespan:** 7-14 days (general), 24 hours (SPA - Microsoft)
- **Format:** Opaque or encrypted JWT
- **Storage:** HttpOnly, Secure, SameSite cookie (BFF) or secure platform storage (mobile)
- **Validation:** Server-side (database lookup)

**ID Tokens:**
- **Lifespan:** Same as access token (15-30 minutes)
- **Format:** JWT (OIDC standard)
- **Purpose:** User identity information only
- **Storage:** Same as access token

**Platform-Specific Lifetimes:**

| Platform | Access Token | Refresh Token | Notes |
|----------|--------------|---------------|-------|
| SPA (BFF pattern) | 15-30 min | 7-14 days | Refresh token server-side only |
| SPA (in-memory) | 15-30 min | Not used | Force re-auth on expiration |
| Mobile App | 15-30 min | 7-14 days | Store in secure platform storage |
| Server-to-Server | 15-30 min | 30-90 days | Higher trust environment |
| Microsoft (SPA) | 15-60 min | 24 hours | Rigid platform defaults |

### Implementing Token Refresh

#### Next.js with NextAuth.js

**Automatic Refresh with Token Rotation:**
```javascript
// pages/api/auth/[...nextauth].js
import NextAuth from "next-auth";
import KeycloakProvider from "next-auth/providers/keycloak";

async function refreshAccessToken(token) {
  try {
    const url = `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`;

    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams({
        client_id: process.env.KEYCLOAK_CLIENT_ID,
        client_secret: process.env.KEYCLOAK_CLIENT_SECRET,
        grant_type: "refresh_token",
        refresh_token: token.refreshToken,
      }),
    });

    const refreshedTokens = await response.json();

    if (!response.ok) {
      throw refreshedTokens;
    }

    return {
      ...token,
      accessToken: refreshedTokens.access_token,
      accessTokenExpires: Date.now() + refreshedTokens.expires_in * 1000,
      refreshToken: refreshedTokens.refresh_token ?? token.refreshToken, // Rotation
    };
  } catch (error) {
    console.error("Error refreshing access token", error);
    return {
      ...token,
      error: "RefreshAccessTokenError",
    };
  }
}

export default NextAuth({
  providers: [
    KeycloakProvider({
      clientId: process.env.KEYCLOAK_CLIENT_ID,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET,
      issuer: process.env.KEYCLOAK_ISSUER,
    }),
  ],
  callbacks: {
    async jwt({ token, account, user }) {
      // Initial sign in
      if (account && user) {
        return {
          accessToken: account.access_token,
          accessTokenExpires: Date.now() + account.expires_in * 1000,
          refreshToken: account.refresh_token,
          user,
        };
      }

      // Return previous token if the access token has not expired
      if (Date.now() < token.accessTokenExpires) {
        return token;
      }

      // Access token has expired, refresh it
      return refreshAccessToken(token);
    },
    async session({ session, token }) {
      session.user = token.user;
      session.accessToken = token.accessToken;
      session.error = token.error;
      return session;
    },
  },
  events: {
    async signOut({ token }) {
      // Revoke tokens on sign out
      const url = `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/logout`;
      await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body: new URLSearchParams({
          client_id: process.env.KEYCLOAK_CLIENT_ID,
          client_secret: process.env.KEYCLOAK_CLIENT_SECRET,
          refresh_token: token.refreshToken,
        }),
      });
    },
  },
});
```

**Client-Side Usage:**
```javascript
// components/ProtectedComponent.tsx
import { useSession } from "next-auth/react";
import { useEffect } from "react";

export default function ProtectedComponent() {
  const { data: session, status } = useSession();

  useEffect(() => {
    // Handle token refresh errors
    if (session?.error === "RefreshAccessTokenError") {
      // Force sign-in
      signIn();
    }
  }, [session]);

  if (status === "loading") {
    return <div>Loading...</div>;
  }

  if (status === "unauthenticated") {
    return <div>Access Denied</div>;
  }

  // Use session.accessToken for API calls
  const fetchData = async () => {
    const response = await fetch('/api/protected-data', {
      headers: {
        'Authorization': `Bearer ${session.accessToken}`,
      },
    });
    return response.json();
  };

  return <div>Protected Content</div>;
}
```

#### Spring Boot Resource Server

**No Refresh Logic Needed:**
- Resource servers validate tokens, don't refresh them
- Client (Next.js) handles token refresh
- Spring Boot validates JWT signature and expiration
- Expired tokens rejected with 401 Unauthorized

**Token Validation:**
```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter =
            new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            grantedAuthoritiesConverter
        );
        return jwtAuthenticationConverter;
    }
}
```

### Refresh Token Revocation

**When to Revoke:**
- User explicitly signs out
- Security breach detected
- Password change
- Account deletion
- Suspicious activity
- Token rotation detects reuse (replay attack)

**Implementation:**
```javascript
// Sign out with token revocation
const handleSignOut = async () => {
  // NextAuth.js automatically calls signOut event
  await signOut({ callbackUrl: '/' });

  // Custom revocation if needed
  await fetch(`${keycloakIssuer}/protocol/openid-connect/logout`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      client_id: clientId,
      client_secret: clientSecret,
      refresh_token: refreshToken,
    }),
  });
};
```

**Revocation Endpoint (RFC 7009):**
```
POST /realms/{realm}/protocol/openid-connect/revoke
Content-Type: application/x-www-form-urlencoded

token={refresh_token}&
client_id={client_id}&
client_secret={client_secret}&
token_type_hint=refresh_token
```

### Cross-Account Protection (RISC)

**Google's RISC Service:**
- Real-time security event notifications
- Notifies applications of account changes
- Events: account disabled, session revoked, password changed
- Enables proactive token revocation

**Integration:**
```javascript
// RISC event receiver endpoint
app.post('/security-events', (req, res) => {
  const securityEvent = req.body;

  if (securityEvent.events['https://schemas.openid.net/secevent/risc/event-type/sessions-revoked']) {
    const subject = securityEvent.subject.email;
    // Revoke all tokens for this user
    revokeAllUserTokens(subject);
  }

  res.status(200).send();
});
```

---

## 7. Recommended Architecture for Next.js + Spring Boot + Keycloak

### Overall Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                             User Browser                             │
└────────────────┬────────────────────────────────────────────────────┘
                 │
                 │ (1) Access Application
                 ▼
        ┌────────────────┐
        │   Next.js App  │
        │   (Frontend)   │
        │                │
        │  - React UI    │
        │  - NextAuth.js │
        │  - BFF Pattern │
        └───┬────────┬───┘
            │        │
            │        │ (2) Authentication Request
            │        ▼
            │   ┌──────────────┐
            │   │   Keycloak   │
            │   │ (Auth Server)│
            │   │              │
            │   │ - User Store │
            │   │ - Token Mgmt │
            │   │ - IdP Broker │
            │   └───┬──────┬───┘
            │       │      │
            │       │      │ (3) Federated Auth
            │       │      ▼
            │       │  ┌──────────────────┐
            │       │  │ Google Identity  │
            │       │  │    Provider      │
            │       │  │                  │
            │       │  │ - OAuth 2.0/OIDC │
            │       │  │ - User AuthN     │
            │       │  └──────────────────┘
            │       │
            │       │ (4) Keycloak JWT
            │       │
            │ (5) API Request with JWT
            ▼
    ┌────────────────────┐
    │   Spring Boot API  │
    │ (Resource Server)  │
    │                    │
    │ - REST Endpoints   │
    │ - JWT Validation   │
    │ - Business Logic   │
    │ - Database Access  │
    └────────────────────┘
```

### Authentication Flow Details

**Step-by-Step Flow:**

1. **User Access:**
   - User visits Next.js app
   - App detects unauthenticated state
   - Shows login page with "Sign in with Google" button

2. **NextAuth.js Initiation:**
   ```javascript
   // User clicks "Sign in with Google"
   signIn('keycloak')
   ```
   - NextAuth.js redirects to Keycloak
   - Includes: `client_id`, `redirect_uri`, `response_type=code`, `scope=openid profile email`
   - PKCE: `code_challenge`, `code_challenge_method=S256`
   - CSRF: `state` parameter

3. **Keycloak Identity Brokering:**
   - Keycloak receives authentication request
   - Detects user needs Google authentication
   - Redirects to Google with its own OAuth parameters
   - Includes Keycloak's Google client credentials

4. **Google Authentication:**
   - User sees Google login page
   - User authenticates with Google credentials
   - User approves scopes (if first time)
   - Google returns authorization code to Keycloak

5. **Keycloak Token Exchange:**
   - Keycloak exchanges authorization code for Google tokens
   - Keycloak validates Google ID token
   - Keycloak creates/updates user in its database
   - User mapping: Google profile → Keycloak user attributes

6. **Keycloak User Session:**
   - Keycloak establishes user session
   - Generates Keycloak-specific tokens:
     - Access Token (JWT) - 15-30 min
     - Refresh Token - 7-14 days
     - ID Token (JWT) - contains user claims
   - Redirects to Next.js callback with authorization code

7. **NextAuth.js Token Exchange:**
   ```javascript
   // NextAuth.js exchanges authorization code for tokens
   POST {keycloak}/protocol/openid-connect/token
   {
     code: authorization_code,
     client_id: nextjs_client_id,
     client_secret: nextjs_client_secret,
     grant_type: 'authorization_code',
     redirect_uri: callback_url,
     code_verifier: pkce_verifier  // PKCE
   }
   ```

8. **Session Establishment:**
   - NextAuth.js receives Keycloak tokens
   - Stores tokens server-side (BFF pattern)
   - Creates session cookie (HttpOnly, Secure, SameSite)
   - Redirects user to application

9. **API Requests:**
   ```javascript
   // Next.js API route
   const session = await getServerSession();
   const response = await fetch('http://spring-boot-api/resource', {
     headers: {
       'Authorization': `Bearer ${session.accessToken}`
     }
   });
   ```

10. **Spring Boot Validation:**
    - Receives request with JWT access token
    - Validates JWT signature using Keycloak's public key (JWKS)
    - Checks token expiration
    - Extracts user claims and roles
    - Processes request if valid
    - Returns 401 if expired/invalid

11. **Token Refresh (When Expired):**
    - NextAuth.js detects token expiration
    - Automatically refreshes using refresh token
    - Gets new access token + new refresh token (rotation)
    - Updates session
    - Retries API request
    - User continues without interruption

### Configuration Files

#### Next.js Environment Variables

```env
# .env.local
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=generate-with-openssl-rand-base64-32

KEYCLOAK_CLIENT_ID=nextjs-app
KEYCLOAK_CLIENT_SECRET=your-secret-from-keycloak
KEYCLOAK_ISSUER=http://localhost:8080/realms/demo

# Only if calling Google APIs directly (not needed for auth via Keycloak)
# GOOGLE_CLIENT_ID=
# GOOGLE_CLIENT_SECRET=
```

#### Spring Boot Configuration

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/demo
          # Keycloak will auto-configure JWKS endpoint

server:
  port: 8081

# CORS Configuration
cors:
  allowed-origins: http://localhost:3000
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allowed-headers: "*"
  allow-credentials: true
```

#### Keycloak Realm Configuration

**Realm Settings:**
```json
{
  "realm": "demo",
  "enabled": true,
  "sslRequired": "external",
  "registrationAllowed": false,
  "loginWithEmailAllowed": true,
  "duplicateEmailsAllowed": false,
  "resetPasswordAllowed": true,
  "editUsernameAllowed": false,

  "accessTokenLifespan": 1800,
  "accessTokenLifespanForImplicitFlow": 900,
  "ssoSessionIdleTimeout": 1800,
  "ssoSessionMaxLifespan": 36000,

  "refreshTokenMaxReuse": 0,
  "revokeRefreshToken": true,
  "refreshTokenExpiration": 1209600
}
```

**Client Configuration (Next.js):**
```json
{
  "clientId": "nextjs-app",
  "enabled": true,
  "clientAuthenticatorType": "client-secret",
  "secret": "your-client-secret",
  "redirectUris": [
    "http://localhost:3000/api/auth/callback/keycloak"
  ],
  "webOrigins": [
    "http://localhost:3000"
  ],
  "protocol": "openid-connect",
  "publicClient": false,
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": false,
  "implicitFlowEnabled": false,
  "serviceAccountsEnabled": false,

  "attributes": {
    "pkce.code.challenge.method": "S256"
  }
}
```

**Identity Provider (Google):**
```json
{
  "alias": "google",
  "providerId": "google",
  "enabled": true,
  "trustEmail": true,
  "storeToken": false,
  "config": {
    "clientId": "your-google-client-id",
    "clientSecret": "your-google-client-secret",
    "defaultScope": "openid profile email",
    "hostedDomain": ""
  }
}
```

### Security Considerations

**Network Security:**
- Use HTTPS in production (all endpoints)
- Configure proper CORS policies
- Implement rate limiting
- Use Web Application Firewall (WAF)

**Token Security:**
- Never expose refresh tokens to client-side JavaScript
- Use HttpOnly cookies for session tokens
- Implement proper token revocation
- Monitor for suspicious token usage

**Session Security:**
- Implement absolute session timeout
- Use SameSite cookie attribute
- Implement CSRF protection
- Monitor concurrent sessions

**Logging and Monitoring:**
- Log all authentication events
- Monitor failed login attempts
- Track token refresh patterns
- Alert on anomalous behavior

---

## 8. Implementation Checklist

### Phase 1: Google OAuth Setup

- [ ] Create project in Google Cloud Console
- [ ] Configure OAuth consent screen
  - [ ] Add required scopes: openid, profile, email
  - [ ] Add test users (during development)
  - [ ] Configure branding (logo, privacy policy, terms of service)
- [ ] Create OAuth 2.0 Client ID (Web application)
- [ ] Configure authorized redirect URIs
- [ ] Store Client ID and Secret in secret manager

### Phase 2: Keycloak Setup

- [ ] Install/Deploy Keycloak
  - [ ] Docker: `docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:latest start-dev`
  - [ ] Production: Use proper deployment (Kubernetes, VM, etc.)
- [ ] Create new realm (or use default)
- [ ] Configure realm settings
  - [ ] Token lifespans
  - [ ] Refresh token rotation
  - [ ] Session settings
- [ ] Add Google Identity Provider
  - [ ] Enter Google Client ID and Secret
  - [ ] Configure scopes: openid profile email
  - [ ] Set hosted domain (if restricting to workspace)
- [ ] Create client for Next.js
  - [ ] Client ID: nextjs-app
  - [ ] Client Protocol: openid-connect
  - [ ] Access Type: confidential
  - [ ] Valid Redirect URIs: http://localhost:3000/api/auth/callback/keycloak
  - [ ] Enable PKCE
- [ ] Create client for Spring Boot (bearer-only)
  - [ ] Client ID: spring-backend
  - [ ] Access Type: bearer-only
- [ ] Configure user federation (if needed)
- [ ] Set up roles and permissions

### Phase 3: Next.js Frontend

- [ ] Install dependencies
  ```bash
  npm install next-auth
  ```
- [ ] Create NextAuth.js configuration
  - [ ] pages/api/auth/[...nextauth].js
  - [ ] Configure Keycloak provider
  - [ ] Implement jwt callback (token storage)
  - [ ] Implement session callback
  - [ ] Implement token refresh logic
  - [ ] Configure events (signOut)
- [ ] Configure environment variables
  - [ ] NEXTAUTH_URL
  - [ ] NEXTAUTH_SECRET
  - [ ] KEYCLOAK_CLIENT_ID
  - [ ] KEYCLOAK_CLIENT_SECRET
  - [ ] KEYCLOAK_ISSUER
- [ ] Create login page
  - [ ] Sign in button
  - [ ] Error handling
- [ ] Implement session provider
  ```javascript
  // pages/_app.js
  import { SessionProvider } from "next-auth/react"

  export default function App({ Component, pageProps: { session, ...pageProps } }) {
    return (
      <SessionProvider session={session}>
        <Component {...pageProps} />
      </SessionProvider>
    )
  }
  ```
- [ ] Protect routes with middleware
- [ ] Create protected pages
- [ ] Implement API routes with authentication
- [ ] Test token refresh flow
- [ ] Implement error handling (RefreshAccessTokenError)

### Phase 4: Spring Boot Backend

- [ ] Add dependencies
  ```xml
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  ```
- [ ] Configure OAuth2 Resource Server
  - [ ] application.yml with issuer-uri
  - [ ] CORS configuration
- [ ] Create Security Configuration
  - [ ] SecurityFilterChain bean
  - [ ] JWT authentication converter
  - [ ] Role mapping (Keycloak roles)
- [ ] Protect endpoints with @PreAuthorize
- [ ] Create REST controllers
- [ ] Test JWT validation
  - [ ] Valid token: 200 OK
  - [ ] Expired token: 401 Unauthorized
  - [ ] Invalid token: 401 Unauthorized
  - [ ] Missing token: 401 Unauthorized
- [ ] Implement role-based access control
- [ ] Test CORS with Next.js frontend

### Phase 5: Security Hardening

- [ ] Implement PKCE
  - [ ] Frontend: Generate code verifier and challenge
  - [ ] Keycloak: Enable PKCE validation
- [ ] Configure state parameter validation
- [ ] Implement token rotation
  - [ ] Keycloak: Enable refresh token rotation
  - [ ] Next.js: Handle new refresh tokens
- [ ] Secure token storage
  - [ ] BFF pattern implementation
  - [ ] HttpOnly cookies
  - [ ] Secure flag (HTTPS)
  - [ ] SameSite attribute
- [ ] Implement token revocation
  - [ ] Sign-out endpoint
  - [ ] Revoke refresh token on logout
- [ ] Configure HTTPS
  - [ ] Development: Local certificates
  - [ ] Production: Let's Encrypt or commercial certificate
- [ ] Implement rate limiting
- [ ] Add security headers
  - [ ] Content-Security-Policy
  - [ ] X-Frame-Options
  - [ ] X-Content-Type-Options
  - [ ] Strict-Transport-Security
- [ ] Configure CORS properly
- [ ] Implement CSRF protection

### Phase 6: Testing

- [ ] Unit tests
  - [ ] Token refresh logic
  - [ ] JWT validation
  - [ ] Role extraction
- [ ] Integration tests
  - [ ] Full authentication flow
  - [ ] Token refresh flow
  - [ ] API access with valid token
  - [ ] API rejection with invalid token
- [ ] Security tests
  - [ ] CSRF protection
  - [ ] Token expiration
  - [ ] Refresh token rotation
  - [ ] PKCE validation
  - [ ] State parameter validation
- [ ] End-to-end tests
  - [ ] User login flow
  - [ ] Access protected resources
  - [ ] Token refresh
  - [ ] Logout
- [ ] Load testing
  - [ ] Token validation performance
  - [ ] Concurrent users

### Phase 7: Monitoring and Logging

- [ ] Implement logging
  - [ ] Authentication events
  - [ ] Token refresh events
  - [ ] Failed authentication attempts
  - [ ] API access logs
- [ ] Set up monitoring
  - [ ] Token expiration alerts
  - [ ] Failed login rate
  - [ ] Suspicious activity detection
- [ ] Implement audit trail
  - [ ] User actions
  - [ ] Administrative changes
- [ ] Configure alerting
  - [ ] Multiple failed logins
  - [ ] Token anomalies
  - [ ] System errors

### Phase 8: Documentation

- [ ] Document architecture
- [ ] Create setup guide
- [ ] Document configuration
- [ ] Create troubleshooting guide
- [ ] Document security practices
- [ ] Create runbooks for common operations

---

## 9. Common Pitfalls and Solutions

### Pitfall 1: Token Storage in Local Storage

**Problem:**
Storing JWT tokens in browser's Local Storage or Session Storage exposes them to XSS attacks.

**Solution:**
Use BFF pattern with HttpOnly cookies or in-memory storage with forced re-authentication.

### Pitfall 2: CORS Preflight Failures

**Problem:**
Spring Boot rejects OPTIONS requests before CORS filter processes them.

**Solution:**
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        return http.build();
    }
}
```

### Pitfall 3: Refresh Token Not Rotating

**Problem:**
Reusing the same refresh token violates OAuth 2.1 security requirements.

**Solution:**
Enable refresh token rotation in Keycloak:
```
Realm → Tokens → Revoke Refresh Token: ON
Realm → Tokens → Refresh Token Max Reuse: 0
```

### Pitfall 4: Hardcoded Credentials

**Problem:**
Client secrets committed to version control.

**Solution:**
- Use environment variables
- Implement secret managers
- Add .env* to .gitignore
- Use different credentials per environment

### Pitfall 5: Missing PKCE Validation

**Problem:**
Authorization code interception attacks possible without PKCE.

**Solution:**
Enable PKCE in both client and server:
```javascript
// NextAuth.js automatically handles PKCE

// Keycloak client configuration
attributes: {
  "pkce.code.challenge.method": "S256"
}
```

### Pitfall 6: Overly Long Token Lifespans

**Problem:**
Long-lived access tokens increase security risk.

**Solution:**
- Access tokens: 15-30 minutes maximum
- Refresh tokens: 7-14 days maximum
- Implement automatic token refresh

### Pitfall 7: No Token Revocation on Logout

**Problem:**
Tokens remain valid after user logs out.

**Solution:**
Implement proper logout with token revocation:
```javascript
events: {
  async signOut({ token }) {
    await fetch(`${keycloak}/protocol/openid-connect/logout`, {
      method: 'POST',
      body: new URLSearchParams({
        client_id: clientId,
        client_secret: clientSecret,
        refresh_token: token.refreshToken,
      }),
    });
  },
}
```

### Pitfall 8: Incorrect Role Mapping

**Problem:**
Keycloak roles not properly extracted in Spring Boot.

**Solution:**
Implement custom JWT authentication converter:
```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
    converter.setAuthoritiesClaimName("realm_access.roles");
    converter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
    jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
    return jwtConverter;
}
```

---

## 10. References and Resources

### Official Documentation

**Google:**
- Google OAuth 2.0 Best Practices: https://developers.google.com/identity/protocols/oauth2/resources/best-practices
- Google Identity Services Overview: https://developers.google.com/identity/gsi/web/guides/overview
- Migration Guide: https://developers.google.com/identity/gsi/web/guides/migration
- OAuth 2.0 for Mobile & Desktop Apps: https://developers.google.com/identity/protocols/oauth2/native-app

**Spring Security:**
- OAuth 2.0 Resource Server JWT: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- OAuth2 Login: https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html
- Spring Boot OAuth2: https://spring.io/guides/tutorials/spring-boot-oauth2/

**Keycloak:**
- Server Administration Guide: https://www.keycloak.org/docs/latest/server_admin/
- Securing Applications: https://www.keycloak.org/docs/latest/securing_apps/
- Identity Brokering: https://www.keycloak.org/docs/latest/server_admin/#_identity_broker

**NextAuth.js / Auth.js:**
- NextAuth.js Documentation: https://next-auth.js.org/
- Keycloak Provider: https://next-auth.js.org/providers/keycloak
- Refresh Token Rotation: https://authjs.dev/guides/refresh-token-rotation
- Third-Party Backend Integration: https://authjs.dev/guides/integrating-third-party-backends

**OAuth 2.0 / OIDC Standards:**
- OAuth 2.0 RFC 6749: https://datatracker.ietf.org/doc/html/rfc6749
- OAuth 2.1: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-11
- PKCE RFC 7636: https://datatracker.ietf.org/doc/html/rfc7636
- OAuth 2.0 Security Best Current Practice: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics
- Token Revocation RFC 7009: https://datatracker.ietf.org/doc/html/rfc7009
- OpenID Connect Core: https://openid.net/specs/openid-connect-core-1_0.html

### Tutorials and Articles (2024-2025)

- OAuth 2.1 and Modern Authentication Patterns (December 2025): https://www.javacodegeeks.com/2025/12/oauth-2-1-and-modern-authentication-patterns-whats-deprecated-and-whats-recommended.html
- Securing Java Applications with Keycloak (February 2025): https://binaryscripts.com/java/2025/02/16/securing-java-applications-with-keycloak.html
- Securing Spring Boot with JWT and OAuth2 (January 2025): https://binaryscripts.com/springboot/2025/01/02/securing-spring-boot-applications-with-jwt-and-oauth2.html
- Zero-Trust API Access with OAuth2 & JWT (August 2025): https://www.javacodegeeks.com/2025/08/secure-yet-developer-friendly-zero-trust-api-access-with-oauth2-jwt-in-spring-boot.html
- OAuth 2.0 Authentication in Spring Boot (2024): https://dev.to/ayshriv/oauth-20-authentication-in-spring-boot-a-guide-to-integrating-google-and-github-login-2hga

### Tools and Libraries

- NextAuth.js: https://next-auth.js.org/
- Spring Security: https://spring.io/projects/spring-security
- Keycloak: https://www.keycloak.org/
- Google Identity Services: https://developers.google.com/identity/gsi/web

### Security Resources

- OWASP OAuth 2.0 Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/OAuth2_Cheat_Sheet.html
- OAuth.net: https://oauth.net/
- OpenID Foundation: https://openid.net/

---

## Next Steps

1. **Review this research document** - Ensure understanding of all concepts
2. **Create detailed implementation plan** - Break down into specific tasks
3. **Set up development environment** - Install required tools and dependencies
4. **Configure Google Cloud Console** - Create OAuth client
5. **Deploy Keycloak** - Local Docker or cloud deployment
6. **Implement Next.js authentication** - NextAuth.js + Keycloak
7. **Implement Spring Boot resource server** - JWT validation
8. **Security hardening** - PKCE, token rotation, HTTPS
9. **Testing** - Comprehensive test coverage
10. **Documentation** - Architecture, setup, troubleshooting guides

---

**Document Version:** 1.0
**Last Updated:** December 16, 2025
**Author:** Research compiled from official documentation and industry best practices

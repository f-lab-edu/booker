# Google OAuth2 Direct Integration Plan (2025)
## Next.js + Spring Boot WITHOUT Keycloak

**Last Updated**: December 16, 2025
**Status**: Planning Phase - Awaiting Approval

---

## Executive Summary

This plan implements direct Google OAuth2 authentication using Google Identity Services (GIS) without any intermediary auth servers like Keycloak. The architecture is simple: Next.js frontend authenticates with Google, receives an ID token, and Spring Boot validates it directly using Google's public certificates.

### Key Architecture Decision
**Simple & Direct**: Next.js → Google OAuth → Spring Boot validates Google ID token

---

## 1. Technology Stack (2025 Latest)

### Frontend (Next.js)
- **Primary Library**: `@react-oauth/google` (v0.12.2+)
  - Based on Google Identity Services (GIS), not deprecated gapi
  - Official React wrapper for Google's OAuth2
  - Supports: Sign In button, One Tap, Automatic sign-in

- **Alternative Options**:
  - NextAuth.js v5 (Auth.js) - if need multi-provider support later
  - Direct GIS JavaScript SDK - for maximum control

### Backend (Spring Boot)
- **Spring Security 6.x** (Spring Boot 3.x)
- **OAuth2 Resource Server** with JWT validation
- **Google API Client Library** for token verification
  - Dependency: `com.google.api-client:google-api-client`
  - Use `GoogleIdTokenVerifier` for validation

### Token Strategy
- **ID Tokens** from Google (JWT format)
- **Session Management**: JWT in HttpOnly cookies (recommended for security)
- **Alternative**: Server-side sessions with Redis (if need instant revocation)

---

## 2. Authentication Flow Architecture

### 2.1 Initial Sign-In Flow

```
User → Next.js App
  ↓
Next.js loads @react-oauth/google
  ↓
User clicks "Sign in with Google" / One Tap appears
  ↓
Google Authentication (popup or redirect)
  ↓
Google returns ID Token (JWT) to Next.js
  ↓
Next.js sends ID Token to Spring Boot API
  ↓
Spring Boot validates token using GoogleIdTokenVerifier
  - Verify signature with Google's public keys
  - Validate iss, aud, exp claims
  ↓
Spring Boot creates session/JWT
  ↓
Response with HttpOnly cookie to Next.js
  ↓
User authenticated
```

### 2.2 Subsequent API Requests

```
Next.js → Spring Boot API (with session cookie)
  ↓
Spring Boot validates session/JWT
  ↓
Process request
  ↓
Response
```

---

## 3. Frontend Implementation Plan (Next.js)

### Phase 1: Setup & Configuration

#### 3.1 Install Dependencies
```bash
npm install @react-oauth/google@latest
```

#### 3.2 Get Google OAuth Credentials
- Go to Google Cloud Console
- Create OAuth 2.0 Client ID
- Add authorized JavaScript origins:
  - `http://localhost:3000` (development)
  - `https://yourdomain.com` (production)
- Add authorized redirect URIs (if using redirect flow)
- Save Client ID (public, goes in frontend)
- Save Client Secret (sensitive, goes in Spring Boot)

#### 3.3 Environment Variables
```env
# .env.local
NEXT_PUBLIC_GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
```

### Phase 2: Implementation

#### 3.4 Root Layout Setup (App Router)
```typescript
// app/layout.tsx
import { GoogleOAuthProvider } from '@react-oauth/google';

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <GoogleOAuthProvider clientId={process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID}>
          {children}
        </GoogleOAuthProvider>
      </body>
    </html>
  );
}
```

#### 3.5 Login Component
```typescript
// components/GoogleSignIn.tsx
'use client';

import { GoogleLogin, CredentialResponse } from '@react-oauth/google';
import { useRouter } from 'next/navigation';

export default function GoogleSignIn() {
  const router = useRouter();

  const handleSuccess = async (credentialResponse: CredentialResponse) => {
    try {
      // Send Google ID token to Spring Boot backend
      const response = await fetch('http://localhost:8080/api/auth/google', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // Important: allows cookies
        body: JSON.stringify({
          idToken: credentialResponse.credential
        })
      });

      if (response.ok) {
        // Backend sets HttpOnly cookie
        router.push('/dashboard');
      } else {
        console.error('Authentication failed');
      }
    } catch (error) {
      console.error('Error during authentication:', error);
    }
  };

  return (
    <GoogleLogin
      onSuccess={handleSuccess}
      onError={() => console.log('Login Failed')}
      useOneTap // Enable One Tap for better UX
      auto_select // Auto-select if only one account
    />
  );
}
```

#### 3.6 One Tap Implementation (Optional)
```typescript
// hooks/useGoogleOneTap.ts
'use client';

import { useGoogleOneTapLogin } from '@react-oauth/google';

export function useGoogleOneTap() {
  useGoogleOneTapLogin({
    onSuccess: async (credentialResponse) => {
      // Same logic as handleSuccess above
      await fetch('http://localhost:8080/api/auth/google', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ idToken: credentialResponse.credential })
      });
    },
    onError: () => console.log('One Tap Login Failed'),
  });
}
```

#### 3.7 API Client Setup (with credentials)
```typescript
// lib/api.ts
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export async function apiClient(endpoint: string, options: RequestInit = {}) {
  return fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    credentials: 'include', // Always include cookies
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });
}
```

#### 3.8 Logout Component
```typescript
// components/Logout.tsx
'use client';

import { googleLogout } from '@react-oauth/google';

export default function Logout() {
  const handleLogout = async () => {
    // Call backend to clear session
    await fetch('http://localhost:8080/api/auth/logout', {
      method: 'POST',
      credentials: 'include'
    });

    // Clear Google session
    googleLogout();

    // Redirect to login
    window.location.href = '/';
  };

  return <button onClick={handleLogout}>Sign Out</button>;
}
```

---

## 4. Backend Implementation Plan (Spring Boot)

### Phase 1: Dependencies

#### 4.1 Maven Dependencies
```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- OAuth2 Resource Server (for JWT validation) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- Google API Client (for ID token verification) -->
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.8.1</version>
</dependency>
```

#### 4.2 Application Properties
```yaml
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}

google:
  client-id: ${GOOGLE_CLIENT_ID}

server:
  servlet:
    session:
      cookie:
        http-only: true
        secure: true  # HTTPS in production
        same-site: lax
        max-age: 86400  # 24 hours
```

### Phase 2: Core Components

#### 4.3 Google ID Token Verifier Bean
```java
// config/GoogleAuthConfig.java
@Configuration
public class GoogleAuthConfig {

    @Value("${google.client-id}")
    private String googleClientId;

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(
            new NetHttpTransport(),
            JacksonFactory.getDefaultInstance()
        )
        .setAudience(Collections.singletonList(googleClientId))
        .setIssuer("https://accounts.google.com")
        .build();
    }
}
```

#### 4.4 Authentication Service
```java
// service/GoogleAuthService.java
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verifier;
    private final UserRepository userRepository;

    public UserDetails authenticateGoogleUser(String idTokenString)
            throws GeneralSecurityException, IOException {

        // Verify and decode the ID token
        GoogleIdToken idToken = verifier.verify(idTokenString);

        if (idToken == null) {
            throw new InvalidTokenException("Invalid Google ID token");
        }

        // Extract user information
        GoogleIdToken.Payload payload = idToken.getPayload();
        String googleId = payload.getSubject();  // Unique Google user ID
        String email = payload.getEmail();
        Boolean emailVerified = payload.getEmailVerified();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        // Require verified email
        if (!emailVerified) {
            throw new UnverifiedEmailException("Email not verified");
        }

        // Optional: Restrict to specific domain
        // String hostedDomain = (String) payload.get("hd");
        // if (!"yourdomain.com".equals(hostedDomain)) {
        //     throw new UnauthorizedDomainException();
        // }

        // Find or create user
        User user = userRepository.findByGoogleId(googleId)
            .orElseGet(() -> createNewUser(googleId, email, name, pictureUrl));

        return new CustomUserDetails(user);
    }

    private User createNewUser(String googleId, String email,
                               String name, String pictureUrl) {
        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName(name);
        user.setPictureUrl(pictureUrl);
        user.setAuthProvider(AuthProvider.GOOGLE);
        return userRepository.save(user);
    }
}
```

#### 4.5 Authentication Controller
```java
// controller/AuthController.java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleAuthService googleAuthService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateGoogle(
            @RequestBody GoogleAuthRequest request,
            HttpSession session) {

        try {
            UserDetails userDetails = googleAuthService
                .authenticateGoogleUser(request.getIdToken());

            // Create authentication
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Store in session (automatically creates JSESSIONID cookie)
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
            );

            return ResponseEntity.ok(new AuthResponse(
                true,
                "Authentication successful",
                userDetails.getUsername()
            ));

        } catch (GeneralSecurityException | IOException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse(false, "Invalid token", null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user")
    public ResponseEntity<UserInfo> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(UserInfo.from(userDetails.getUser()));
    }
}
```

#### 4.6 Security Configuration
```java
// config/SecurityConfig.java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/auth/**")  // For initial auth
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/google", "/api/auth/logout").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                )
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // Development
            "https://yourdomain.com"  // Production
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);  // Important for cookies
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

#### 4.7 User Entity
```java
// entity/User.java
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String googleId;  // Google's unique 'sub' claim

    @Column(unique = true, nullable = false)
    private String email;

    private String name;
    private String pictureUrl;

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

---

## 5. Token Validation Deep Dive

### 5.1 What GoogleIdTokenVerifier Does Automatically

The `GoogleIdTokenVerifier` from Google's API client library:

1. **Fetches Google's Public Keys**
   - URL: `https://www.googleapis.com/oauth2/v3/certs`
   - Caches them according to Cache-Control header
   - Auto-rotates when expired

2. **Verifies JWT Signature**
   - Uses RS256 algorithm
   - Validates against Google's public RSA keys

3. **Validates Claims**
   - `iss`: Must be "https://accounts.google.com" or "accounts.google.com"
   - `aud`: Must match your client ID
   - `exp`: Token not expired
   - `iat`: Issued at time is valid

### 5.2 Manual Validation (If Not Using Google Library)

If you want to use Spring Security's built-in JWT decoder:

```java
@Bean
public JwtDecoder jwtDecoder() {
    // Google's JWK Set endpoint
    String jwkSetUri = "https://www.googleapis.com/oauth2/v3/certs";

    NimbusJwtDecoder decoder = NimbusJwtDecoder
        .withJwkSetUri(jwkSetUri)
        .build();

    // Add custom validators
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        new JwtTimestampValidator(),
        new JwtIssuerValidator("https://accounts.google.com"),
        new JwtClaimValidator<String>("aud",
            aud -> aud.equals(googleClientId))
    ));

    return decoder;
}
```

---

## 6. Session Management Strategy

### Option A: Session-Based (Recommended for MVP)

**Pros:**
- Easy to invalidate (instant logout)
- Familiar pattern
- Built into Spring Security
- Better security (can't steal/reuse session)

**Cons:**
- Requires sticky sessions in distributed systems
- Needs Redis/database for scaling

**Implementation:**
- Use Spring Session with Redis for distributed sessions
- HttpOnly, Secure, SameSite=Lax cookies
- 24-hour session timeout
- Auto-extend on activity

### Option B: JWT in Cookies (For Stateless)

**Pros:**
- Stateless (easy to scale horizontally)
- No server-side storage needed
- Works well with microservices

**Cons:**
- Cannot instantly revoke (must wait for expiration)
- Larger cookie size
- More complex refresh token logic

**Implementation:**
- Create custom JWT after Google validation
- Store in HttpOnly cookie
- Short expiration (15-30 minutes)
- Refresh token mechanism required

### Recommendation
**Start with Option A (Session-Based)** for simplicity. Migrate to JWT later if needed for scaling.

---

## 7. Token Refresh Strategy (Future Enhancement)

Google ID tokens are short-lived (typically 1 hour). For long-lived sessions:

### 7.1 Backend Session Approach (Simpler)
```java
// Store refresh token in session when available
// Google only provides refresh token on first consent with offline_access scope
session.setAttribute("google_refresh_token", refreshToken);

// When ID token expires, use refresh token to get new one
// This happens transparently in the session
```

### 7.2 Frontend Refresh Flow (For JWT approach)
```typescript
// Automatic token refresh before expiration
useEffect(() => {
  const refreshInterval = setInterval(async () => {
    await fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include'
    });
  }, 14 * 60 * 1000); // Every 14 minutes for 15-min tokens

  return () => clearInterval(refreshInterval);
}, []);
```

### 7.3 Getting Refresh Tokens from Google

To get a refresh token from Google (for server-side token refresh):

```typescript
// Frontend: Request offline access
const login = useGoogleLogin({
  onSuccess: async (codeResponse) => {
    // Send authorization code to backend
    await fetch('/api/auth/google', {
      method: 'POST',
      body: JSON.stringify({ code: codeResponse.code })
    });
  },
  flow: 'auth-code',
  scope: 'openid email profile',
  access_type: 'offline',  // Request refresh token
  prompt: 'consent'        // Force consent to get refresh token
});
```

```java
// Backend: Exchange code for tokens including refresh token
GoogleAuthorizationCodeTokenRequest tokenRequest =
    new GoogleAuthorizationCodeTokenRequest(
        new NetHttpTransport(),
        JacksonFactory.getDefaultInstance(),
        "https://oauth2.googleapis.com/token",
        clientId,
        clientSecret,
        code,
        redirectUri
    );

GoogleTokenResponse tokenResponse = tokenRequest.execute();
String idToken = tokenResponse.getIdToken();
String accessToken = tokenResponse.getAccessToken();
String refreshToken = tokenResponse.getRefreshToken();  // Store this securely

// Use refresh token later to get new tokens
GoogleRefreshTokenRequest refreshRequest =
    new GoogleRefreshTokenRequest(
        new NetHttpTransport(),
        JacksonFactory.getDefaultInstance(),
        refreshToken,
        clientId,
        clientSecret
    );
GoogleTokenResponse newTokenResponse = refreshRequest.execute();
```

**Important Notes:**
- Refresh tokens only provided on first consent or with `prompt=consent`
- Must use `access_type=offline` and authorization code flow
- Store refresh tokens securely (encrypted in database)
- Refresh tokens can be long-lived (no expiration) or expire after 6 months of non-use

---

## 8. Security Best Practices

### 8.1 Frontend Security
- ✅ Never store tokens in localStorage (XSS vulnerable)
- ✅ Always use HttpOnly cookies for session/JWT
- ✅ Set SameSite=Lax or Strict on cookies (CSRF protection)
- ✅ Use Secure flag in production (HTTPS only)
- ✅ Implement CSRF tokens for state-changing requests
- ✅ Call `googleLogout()` on user logout to clear Google session
- ✅ Validate HTTPS in production

### 8.2 Backend Security
- ✅ Always verify Google ID token signature with Google's public keys
- ✅ Validate all JWT claims (iss, aud, exp)
- ✅ Use Google's `sub` claim as unique user identifier (NOT email)
- ✅ Require email verification (`email_verified` claim)
- ✅ Optional: Validate `hd` (hosted domain) for Google Workspace restrictions
- ✅ Never expose Google Client Secret to frontend
- ✅ Use HTTPS in production
- ✅ Implement rate limiting on auth endpoints
- ✅ Log authentication attempts for monitoring
- ✅ Set appropriate CORS policies (specific origins, not *)
- ✅ Rotate secrets regularly
- ✅ Implement session timeout (24 hours recommended)

### 8.3 Token Storage
- ✅ Store refresh tokens encrypted in database
- ✅ Never log tokens
- ✅ Delete tokens when user logs out
- ✅ Implement token revocation mechanism
- ✅ Use secure random generation for session IDs

### 8.4 Google-Specific Security
- ✅ Register authorized JavaScript origins in Google Console
- ✅ Use most restrictive scopes needed (openid, email, profile)
- ✅ Implement incremental authorization (request scopes when needed)
- ✅ Monitor Google Cloud Console for suspicious activity
- ✅ Enable Google's Cross-Account Protection for revocation events

### 8.5 CORS Configuration
```java
// Specific origins only
.setAllowedOrigins(Arrays.asList("https://yourdomain.com"))
// Not: .setAllowedOrigins(Arrays.asList("*"))

// Allow credentials (required for cookies)
.setAllowCredentials(true)
```

### 8.6 CSRF Protection
```java
// Use double-submit cookie pattern
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
)

// Frontend: Include CSRF token in requests
fetch('/api/books', {
  headers: {
    'X-XSRF-TOKEN': getCsrfToken()
  }
})
```

---

## 9. Migration from Development to Production

### 9.1 Google Cloud Console Changes
1. Add production domain to Authorized JavaScript Origins
2. Add production API domain to CORS allowed origins
3. Update OAuth Consent Screen with production URLs
4. Consider verified app status for better user trust
5. Remove localhost origins (security)

### 9.2 Environment Variables
```yaml
# Production application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}  # From secrets manager
            client-secret: ${GOOGLE_CLIENT_SECRET}  # From secrets manager

server:
  servlet:
    session:
      cookie:
        secure: true  # HTTPS only
        domain: .yourdomain.com
        same-site: lax
```

### 9.3 Frontend Environment
```env
# .env.production
NEXT_PUBLIC_GOOGLE_CLIENT_ID=your-prod-client-id
NEXT_PUBLIC_API_URL=https://api.yourdomain.com
```

### 9.4 Security Checklist
- [ ] Enable HTTPS on all domains
- [ ] Set secure flag on all cookies
- [ ] Update CORS to production origins only
- [ ] Use secret manager (not hardcoded secrets)
- [ ] Enable rate limiting
- [ ] Set up monitoring and alerting
- [ ] Configure session persistence (Redis)
- [ ] Test token expiration and refresh
- [ ] Verify CSRF protection works
- [ ] Test logout across all devices

---

## 10. Testing Strategy

### 10.1 Frontend Tests
```typescript
// Test GoogleSignIn component
describe('GoogleSignIn', () => {
  it('calls backend API on successful Google login', async () => {
    const mockFetch = jest.fn().mockResolvedValue({ ok: true });
    global.fetch = mockFetch;

    render(<GoogleSignIn />);

    // Simulate Google login success
    const onSuccess = screen.getByTestId('google-login').props.onSuccess;
    await onSuccess({ credential: 'mock-token' });

    expect(mockFetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/auth/google',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ idToken: 'mock-token' })
      })
    );
  });
});
```

### 10.2 Backend Tests
```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleIdTokenVerifier verifier;

    @Test
    void testGoogleAuthentication_Success() throws Exception {
        // Mock Google token verification
        GoogleIdToken mockToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-user-123");
        payload.setEmail("user@example.com");
        payload.setEmailVerified(true);

        when(mockToken.getPayload()).thenReturn(payload);
        when(verifier.verify(anyString())).thenReturn(mockToken);

        // Test authentication endpoint
        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"mock-token\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGoogleAuthentication_InvalidToken() throws Exception {
        when(verifier.verify(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"invalid-token\"}"))
            .andExpect(status().isUnauthorized());
    }
}
```

### 10.3 Integration Tests
- Test complete flow: Google login → token validation → session creation
- Test protected endpoints with and without authentication
- Test logout and session invalidation
- Test CORS with different origins
- Test CSRF protection

### 10.4 Manual Testing Checklist
- [ ] Sign in with Google account
- [ ] Verify session cookie is HttpOnly and Secure
- [ ] Access protected API endpoint
- [ ] Logout and verify session cleared
- [ ] Test with multiple Google accounts
- [ ] Test One Tap login
- [ ] Verify CORS works from Next.js origin
- [ ] Test token expiration handling
- [ ] Test with unverified email (should fail)
- [ ] Test with different browsers

---

## 11. Implementation Phases

### Phase 1: Backend Setup (Days 1-2)
- [ ] Add Maven dependencies
- [ ] Create GoogleAuthConfig with verifier bean
- [ ] Implement User entity and repository
- [ ] Create GoogleAuthService with token verification
- [ ] Implement AuthController with /google and /logout endpoints
- [ ] Configure SecurityConfig with CORS and session management
- [ ] Test token verification with mock tokens

### Phase 2: Frontend Setup (Days 2-3)
- [ ] Install @react-oauth/google
- [ ] Add GoogleOAuthProvider to root layout
- [ ] Create GoogleSignIn component
- [ ] Implement API client with credentials: 'include'
- [ ] Create Logout component
- [ ] Add environment variables
- [ ] Test local Google login flow

### Phase 3: Integration (Day 3)
- [ ] Connect frontend to backend auth endpoints
- [ ] Verify session cookie creation
- [ ] Test protected API calls with session
- [ ] Implement user info endpoint
- [ ] Test complete authentication flow

### Phase 4: Security Hardening (Day 4)
- [ ] Enable CSRF protection (except for /api/auth/*)
- [ ] Configure CORS properly (specific origins)
- [ ] Add rate limiting to auth endpoints
- [ ] Implement request logging
- [ ] Verify all cookies are HttpOnly and Secure
- [ ] Test XSS and CSRF protections

### Phase 5: Optional Enhancements (Days 5-6)
- [ ] Add One Tap login
- [ ] Implement token refresh mechanism
- [ ] Add domain restriction (hd claim)
- [ ] Create user profile page
- [ ] Add remember me functionality
- [ ] Implement session timeout warning

### Phase 6: Testing & Documentation (Day 7)
- [ ] Write unit tests for auth service
- [ ] Write integration tests
- [ ] Manual testing checklist
- [ ] Document API endpoints
- [ ] Create deployment guide
- [ ] Security audit

---

## 12. Common Issues & Solutions

### Issue 1: CORS Errors
**Symptom**: "Access-Control-Allow-Origin" error in browser console

**Solution**:
```java
// Ensure CORS allows credentials and specific origin
.setAllowCredentials(true)
.setAllowedOrigins(Arrays.asList("http://localhost:3000"))

// Frontend must include credentials
fetch(url, { credentials: 'include' })
```

### Issue 2: Cookies Not Being Set
**Symptom**: No session cookie after successful authentication

**Solution**:
- Check `credentials: 'include'` in fetch
- Verify CORS `allowCredentials: true`
- Check cookie domain matches (localhost vs 127.0.0.1)
- Ensure SameSite is 'lax' or 'none' (with Secure)

### Issue 3: Google Token Verification Fails
**Symptom**: GoogleIdTokenVerifier returns null

**Solutions**:
- Verify client ID matches in verifier and Google Console
- Check token hasn't expired (1-hour lifetime)
- Ensure token is from correct Google project
- Verify internet connectivity for public key fetching

### Issue 4: One Tap Not Appearing
**Symptom**: One Tap prompt doesn't show

**Solutions**:
- Call `googleLogout()` to clear previous session
- Check domain is authorized in Google Console
- Ensure user hasn't dismissed One Tap recently (cooldown period)
- Verify cookies are enabled in browser

### Issue 5: Session Lost on Page Refresh
**Symptom**: User logged out after refresh

**Solutions**:
- Verify session cookie has correct max-age
- Check HttpOnly flag isn't preventing JavaScript access (it should)
- Ensure session is properly stored server-side
- Verify browser isn't blocking third-party cookies

---

## 13. Monitoring & Observability

### 13.1 Metrics to Track
- Authentication success/failure rates
- Token verification latency
- Session creation rate
- Active sessions count
- Failed login attempts per IP
- Token expiration events
- Google API public key fetch rate

### 13.2 Logging
```java
@Slf4j
@Service
public class GoogleAuthService {

    public UserDetails authenticateGoogleUser(String idTokenString) {
        try {
            log.info("Attempting Google authentication");
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                log.warn("Invalid Google ID token received");
                throw new InvalidTokenException("Invalid token");
            }

            String email = idToken.getPayload().getEmail();
            log.info("Successfully authenticated user: {}", email);

            return createUserDetails(idToken);

        } catch (Exception e) {
            log.error("Google authentication failed", e);
            throw new AuthenticationException("Authentication failed", e);
        }
    }
}
```

### 13.3 Alerts
- High rate of authentication failures
- Unusual geographic login patterns
- Multiple failed attempts from same IP
- Token verification service downtime
- Session storage issues

---

## 14. Cost Considerations

### Free Tier Limits
- **Google OAuth**: Free for standard OAuth2
- **Google Cloud Console**: Free project creation
- **API Calls**: Token verification is free (public keys cached)

### Potential Costs
- **Session Storage**: Redis/database for distributed sessions
- **Monitoring**: Cloud monitoring services
- **Bandwidth**: API calls between frontend and backend

### Cost Optimization
- Cache Google's public keys (auto-handled by library)
- Use session-based auth to minimize token verifications
- Implement efficient session cleanup
- Use Redis session store with TTL

---

## 15. Future Enhancements

### 15.1 Multi-Provider Support
- Add GitHub OAuth
- Add Microsoft OAuth
- Abstract auth service for multiple providers

### 15.2 Advanced Features
- Two-factor authentication (2FA)
- Social account linking
- Progressive profile completion
- Activity log (login history)
- Device management (active sessions)

### 15.3 Performance
- Implement session Redis cluster
- Add CDN for static assets
- Use JWT for stateless scaling
- Implement token blacklist for instant revocation

### 15.4 Security
- Add anomaly detection
- Implement rate limiting per user
- Add CAPTCHA for suspicious activity
- Integrate with Google's Cross-Account Protection
- Add security headers (CSP, HSTS, etc.)

---

## 16. References & Documentation

### Official Documentation
- **Google Identity Services**: https://developers.google.com/identity/gsi/web/guides/overview
- **Verify Google ID Token**: https://developers.google.com/identity/gsi/web/guides/verify-google-id-token
- **OAuth2 Best Practices**: https://developers.google.com/identity/protocols/oauth2/resources/best-practices
- **Spring Security OAuth2**: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- **@react-oauth/google**: https://github.com/MomenSherif/react-oauth

### Key Libraries
- **Frontend**: @react-oauth/google v0.12.2+
- **Backend**: com.google.api-client:google-api-client:2.8.1
- **Spring Security**: spring-boot-starter-oauth2-resource-server

### Recent Articles (2024-2025)
- Google OAuth in React: https://marmelab.com/blog/2024/11/18/google-authentication-react.html
- JWT vs Session comparison: https://medium.com/@pranavprakash4777/jwt-vs-oauth2-vs-session-cookies
- Spring Boot JWT Auth: https://evoila.com/us/blog/spring-boot-3-jwt-authentication-leveraging-spring-securitys-support/

---

## 17. Decision Log

### Why NOT Keycloak?
- **Complexity**: Keycloak adds another service to manage
- **Overhead**: For single OAuth provider, direct integration is simpler
- **Latency**: Extra hop through Keycloak adds latency
- **Learning Curve**: Team would need to learn Keycloak config
- **Infrastructure**: Requires separate deployment and database
- **Cost**: Additional server resources

**Decision**: Use direct Google OAuth integration. Consider Keycloak later if need:
- Multiple OAuth providers
- Advanced identity management
- SSO across multiple apps
- Complex authorization policies

### Session vs JWT
**Decision**: Use session-based auth (Spring's default) for MVP

**Reasoning**:
- Simpler implementation
- Instant logout capability
- Better security (server-controlled)
- Built into Spring Security
- Can migrate to JWT later if needed

### @react-oauth/google vs NextAuth.js
**Decision**: Use @react-oauth/google

**Reasoning**:
- Simpler for single provider
- Direct control over auth flow
- Lighter weight
- Latest Google Identity Services
- Better for learning OAuth concepts

**Reconsider NextAuth.js if**:
- Need multiple providers (GitHub, Microsoft, etc.)
- Want built-in session management
- Need database session adapter
- Want email/passwordless auth

---

## 18. Questions to Resolve

### Before Implementation
- [ ] What user roles/permissions are needed?
- [ ] Should we restrict to specific Google Workspace domain?
- [ ] What's the session timeout requirement?
- [ ] Need "remember me" functionality?
- [ ] Single session per user or multiple devices?
- [ ] What user data to store locally?
- [ ] Need activity logging?
- [ ] What's the logout behavior (all devices vs current)?

### For Production
- [ ] What's the production domain?
- [ ] Where to deploy (AWS, GCP, Azure, etc.)?
- [ ] What's the SSL certificate plan?
- [ ] Need Redis for sessions or database sufficient?
- [ ] What's the monitoring/logging infrastructure?
- [ ] Backup and disaster recovery plan?
- [ ] GDPR/data privacy requirements?

---

## 19. Success Metrics

### Technical Metrics
- [ ] Authentication flow completes in <2 seconds
- [ ] Token verification latency <200ms
- [ ] Session creation success rate >99%
- [ ] Zero XSS/CSRF vulnerabilities
- [ ] All cookies are HttpOnly and Secure
- [ ] CORS properly configured (no wildcard)

### User Experience Metrics
- [ ] Login button visible and accessible
- [ ] One Tap appears on returning visits
- [ ] Logout clears session completely
- [ ] No unnecessary login prompts
- [ ] Error messages are user-friendly
- [ ] Mobile login works smoothly

### Security Metrics
- [ ] No tokens in localStorage
- [ ] No secrets in frontend code
- [ ] All API calls use credentials: 'include'
- [ ] CSRF protection enabled
- [ ] Rate limiting active on auth endpoints
- [ ] Failed login attempts logged

---

## Conclusion

This plan provides a production-ready approach to implementing Google OAuth2 authentication directly in a Next.js + Spring Boot application without Keycloak. The architecture is simple, secure, and follows 2025 best practices.

**Key Takeaways:**
1. Use **Google Identity Services** (GIS) via @react-oauth/google
2. Validate tokens in Spring Boot using **GoogleIdTokenVerifier**
3. Use **session-based auth** for simplicity and security
4. Implement **HttpOnly cookies** for session storage
5. Follow **OAuth2 security best practices** (CORS, CSRF, HTTPS)
6. Use Google's **sub claim** as unique user identifier
7. Cache Google's public keys automatically via verifier

**Next Steps:**
1. Review and approve this plan
2. Set up Google Cloud Console OAuth credentials
3. Start Phase 1: Backend implementation
4. Test thoroughly before production deployment

---

**Status**: Ready for implementation approval
**Estimated Timeline**: 7 days for full implementation
**Risk Level**: Low (well-established patterns and libraries)

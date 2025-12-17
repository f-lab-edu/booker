# Google OAuth Login Implementation Plan

## Project Overview
Implement Google OAuth login functionality for the BOOKER application using Keycloak as the centralized authentication provider with Google as the Identity Provider.

**Date Created**: 2025-12-16
**Status**: Awaiting Review

---

## Current State Analysis

### What We Have
- ✅ Spring Boot backend with JWT interceptor structure
- ✅ Next.js frontend with mock AuthContext
- ✅ Keycloak infrastructure (separate from docker-compose)
- ✅ API client with Bearer token support
- ✅ MySQL database for application data
- ✅ Basic user/role models in backend

### What's Missing
- ❌ Actual JWT validation logic in backend
- ❌ Keycloak integration on frontend
- ❌ Google OAuth configuration in Keycloak
- ❌ Token refresh mechanism
- ❌ Protected routes and auth guards
- ❌ Proper error handling for auth failures

---

## Architecture Decision

### Selected Approach: **Keycloak-Centric OAuth with NextAuth.js**

```
┌─────────┐    ┌──────────────┐    ┌──────────┐    ┌────────┐    ┌─────────────┐
│  User   │───▶│   Next.js    │───▶│Keycloak  │───▶│ Google │───▶│ Spring Boot │
│ Browser │◀───│(NextAuth.js) │◀───│  Realm   │◀───│  OAuth │◀───│   API       │
└─────────┘    └──────────────┘    └──────────┘    └────────┘    └─────────────┘
```

**Why This Approach?**
1. **Centralized Auth**: Keycloak manages all authentication, even if we add other providers later
2. **Security**: NextAuth.js handles token storage securely with session strategy
3. **Backend Simplicity**: Spring Boot only validates JWT tokens, doesn't handle OAuth flow
4. **User Experience**: Google's native popup login with "One Tap" support
5. **Future-Proof**: Easy to add GitHub, Apple, or custom auth providers

---

## Implementation Plan

### Phase 1: Keycloak Setup and Configuration
**Goal**: Configure Keycloak to act as OAuth provider with Google Identity Provider

#### Task 1.1: Setup Keycloak Service
- [ ] Add Keycloak service to `docker-compose.yml`
  - Image: `quay.io/keycloak/keycloak:23.0`
  - Port: 8083
  - Database: PostgreSQL container for Keycloak data
  - Environment variables for admin credentials
  - Volume mount for persistence

**Files to modify:**
- `/Users/foodtech/Documents/booker/docker-compose.yml`

**Reasoning**: Keycloak needs to be part of the docker-compose stack for consistent development environment. Using PostgreSQL instead of H2 for production-readiness.

#### Task 1.2: Configure Keycloak Realm
- [ ] Create/verify `booker` realm (or use existing `myrealm`)
- [ ] Configure realm settings:
  - Display name: "Booker Authentication"
  - Login theme: keycloak (default)
  - Access token lifespan: 15 minutes
  - Refresh token lifespan: 14 days
  - SSO session idle: 30 minutes

**Access**: http://localhost:8083/admin
**Reasoning**: Shorter token lifespans improve security. Refresh tokens allow seamless re-authentication.

#### Task 1.3: Create Keycloak Client for Next.js
- [ ] Create new client in Keycloak:
  - Client ID: `booker-web`
  - Client Protocol: `openid-connect`
  - Access Type: `confidential`
  - Valid Redirect URIs:
    - `http://localhost:3000/api/auth/callback/keycloak`
    - `http://localhost:3000/*` (for development)
  - Web Origins: `http://localhost:3000`
  - Standard Flow: Enabled (Authorization Code Flow)
  - Direct Access Grants: Disabled (for security)
- [ ] Note the Client Secret for Next.js configuration

**Reasoning**: Confidential client with Authorization Code Flow is the most secure approach for web applications. PKCE will be added by NextAuth.js automatically.

#### Task 1.4: Configure Google Identity Provider in Keycloak
- [ ] Go to Identity Providers → Add provider → Google
- [ ] Configure Google IdP:
  - Alias: `google`
  - Client ID: (from Google Cloud Console)
  - Client Secret: (from Google Cloud Console)
  - Default Scopes: `openid profile email`
  - Store Tokens: Enabled (to allow token refresh)
  - Trust Email: Enabled
- [ ] Configure Mappers to extract user info (email, name, picture)

**Prerequisites**: Google Cloud Console OAuth 2.0 credentials
**Reasoning**: Keycloak brokers the authentication to Google but issues its own tokens for consistent API access.

#### Task 1.5: Setup Google Cloud Console OAuth
- [ ] Create new project or use existing in Google Cloud Console
- [ ] Enable Google+ API (if not already enabled)
- [ ] Create OAuth 2.0 credentials:
  - Application type: Web application
  - Authorized redirect URIs:
    - `http://localhost:8083/realms/booker/broker/google/endpoint`
    - Production URL when deployed
- [ ] Note Client ID and Client Secret for Keycloak config

**Access**: https://console.cloud.google.com/apis/credentials
**Reasoning**: Google requires pre-registered redirect URIs. The Keycloak broker endpoint must be whitelisted.

---

### Phase 2: Backend Spring Boot Configuration
**Goal**: Enable JWT validation for Keycloak-issued tokens

#### Task 2.1: Add OAuth2 Resource Server Dependencies
- [ ] Update `build.gradle`:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
implementation 'org.springframework.boot:spring-boot-starter-security'
```

**File**: `/Users/foodtech/Documents/booker/booker-server/build.gradle`
**Reasoning**: Spring Security's OAuth2 Resource Server provides robust JWT validation with JWKS support.

#### Task 2.2: Configure JWT Validation in application.yml
- [ ] Add Spring Security OAuth2 configuration:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8083/realms/booker
          jwk-set-uri: http://localhost:8083/realms/booker/protocol/openid-connect/certs
```

**File**: `/Users/foodtech/Documents/booker/booker-server/src/main/resources/application.yml`
**Reasoning**: Auto-configures JWT validation with Keycloak's public keys. Spring will automatically fetch and cache JWKS.

#### Task 2.3: Create Security Configuration Class
- [ ] Create `SecurityConfig.java`:
  - Configure HTTP security with JWT authentication
  - Define public endpoints (health checks, Swagger)
  - Define protected endpoints (all /api/v1/*)
  - Configure CORS to allow Next.js origin
  - Custom JWT authentication converter for role extraction

**File**: `/Users/foodtech/Documents/booker/booker-server/src/main/java/com/bookerapp/core/infrastructure/config/SecurityConfig.java`

**Sample Structure**:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Using JWT, not session cookies
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .build();
    }
}
```

**Reasoning**: Declarative security configuration is more maintainable than imperative interceptors. JWT validation is handled by Spring Security filters.

#### Task 2.4: Implement JWT Claims to UserContext Converter
- [ ] Create `KeycloakJwtAuthenticationConverter.java`:
  - Extract `sub` (subject/userId) from JWT
  - Extract `email`, `name`, `preferred_username`
  - Extract roles from `realm_access.roles` claim
  - Map Keycloak roles to Spring Security authorities
  - Create UserContext object

**File**: `/Users/foodtech/Documents/booker/booker-server/src/main/java/com/bookerapp/core/infrastructure/security/KeycloakJwtAuthenticationConverter.java`

**Reasoning**: Keycloak JWT structure differs from standard OAuth2. Custom converter ensures proper role mapping and user context creation.

#### Task 2.5: Update JwtAuthInterceptor (or Remove)
- [ ] Option A: Remove `JwtAuthInterceptor.java` entirely (Spring Security handles it)
- [ ] Option B: Simplify to only log authentication details for debugging

**File**: `/Users/foodtech/Documents/booker/booker-server/src/main/java/com/bookerapp/core/presentation/interceptor/JwtAuthInterceptor.java`

**Reasoning**: With Spring Security OAuth2 Resource Server, the interceptor becomes redundant. Security is handled at the filter level.

#### Task 2.6: Update UserContextArgumentResolver
- [ ] Modify to extract UserContext from Spring Security's SecurityContext:
```java
@Override
public Object resolveArgument(...) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt) {
        return UserContext.fromJwt(jwt.getToken());
    }
    throw new UnauthorizedException("No valid authentication found");
}
```

**File**: `/Users/foodtech/Documents/booker/booker-server/src/main/java/com/bookerapp/core/presentation/argumentresolver/UserContextArgumentResolver.java`

**Reasoning**: Controllers can still use `@AuthUser UserContext user` parameter, but now it extracts from Spring Security's managed authentication.

---

### Phase 3: Frontend Next.js OAuth Integration
**Goal**: Implement Google OAuth login flow using NextAuth.js with Keycloak provider

#### Task 3.1: Install NextAuth.js and Dependencies
- [ ] Install packages:
```bash
npm install next-auth@latest
```

**File**: `/Users/foodtech/Documents/booker/booker-client/package.json`
**Version**: Use v5 (latest stable for Next.js 14+)
**Reasoning**: NextAuth.js handles OAuth flow, session management, and token refresh automatically.

#### Task 3.2: Create NextAuth Configuration
- [ ] Create API route: `app/api/auth/[...nextauth]/route.ts`
- [ ] Configure Keycloak provider:
  - Client ID: `booker-web`
  - Client Secret: (from Keycloak)
  - Issuer: `http://localhost:8083/realms/booker`
- [ ] Configure session strategy (JWT vs database)
- [ ] Implement token refresh callback
- [ ] Add custom JWT callback to include user info

**File**: `/Users/foodtech/Documents/booker/booker-client/src/app/api/auth/[...nextauth]/route.ts`

**Sample Structure**:
```typescript
import NextAuth from "next-auth";
import KeycloakProvider from "next-auth/providers/keycloak";

const handler = NextAuth({
  providers: [
    KeycloakProvider({
      clientId: process.env.KEYCLOAK_CLIENT_ID!,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET!,
      issuer: process.env.KEYCLOAK_ISSUER!,
    }),
  ],
  callbacks: {
    async jwt({ token, account }) {
      // Persist access_token and refresh_token
      if (account) {
        token.accessToken = account.access_token;
        token.refreshToken = account.refresh_token;
        token.expiresAt = account.expires_at;
      }
      return token;
    },
    async session({ session, token }) {
      // Send accessToken to client
      session.accessToken = token.accessToken;
      session.user = {
        ...session.user,
        id: token.sub,
      };
      return session;
    },
  },
});

export { handler as GET, handler as POST };
```

**Reasoning**: NextAuth.js Keycloak provider automatically handles OAuth flow, PKCE, and redirects. JWT strategy avoids database dependency.

#### Task 3.3: Setup Environment Variables
- [ ] Create `.env.local`:
```env
KEYCLOAK_CLIENT_ID=booker-web
KEYCLOAK_CLIENT_SECRET=<from-keycloak>
KEYCLOAK_ISSUER=http://localhost:8083/realms/booker
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=<generate-random-secret>
```

**File**: `/Users/foodtech/Documents/booker/booker-client/.env.local`
**Security**: Add `.env.local` to `.gitignore` (should already be there)
**Reasoning**: Environment variables keep secrets out of source code. NEXTAUTH_SECRET is used to encrypt session tokens.

#### Task 3.4: Replace AuthContext with NextAuth Session Provider
- [ ] Update `app/providers.tsx`:
  - Remove old AuthProvider
  - Add NextAuth SessionProvider
  - Wrap React Query provider

**File**: `/Users/foodtech/Documents/booker/booker-client/src/app/providers.tsx`

```typescript
import { SessionProvider } from "next-auth/react";

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <SessionProvider>
      <QueryClientProvider client={queryClient}>
        {children}
      </QueryClientProvider>
    </SessionProvider>
  );
}
```

**Reasoning**: SessionProvider makes authentication state available throughout the app via `useSession()` hook.

#### Task 3.5: Update Header Component with Login Button
- [ ] Modify `components/layout/Header.tsx`:
  - Use `useSession()` to check auth state
  - Replace "Login" button with:
    - "Sign in with Google" when logged out
    - User avatar + dropdown when logged in
  - Call `signIn("keycloak")` on button click
  - Call `signOut()` on logout click
  - Display Google popup automatically

**File**: `/Users/foodtech/Documents/booker/booker-client/src/components/layout/Header.tsx`

**Sample Implementation**:
```typescript
import { useSession, signIn, signOut } from "next-auth/react";

export function Header() {
  const { data: session, status } = useSession();

  if (status === "loading") return <div>Loading...</div>;

  return (
    <header>
      {/* Navigation items */}
      {session ? (
        <div>
          <span>{session.user?.name}</span>
          <button onClick={() => signOut()}>Sign Out</button>
        </div>
      ) : (
        <button onClick={() => signIn("keycloak")}>
          Login
        </button>
      )}
    </header>
  );
}
```

**Reasoning**: `signIn("keycloak")` triggers OAuth flow which redirects to Keycloak, which then shows Google login popup.

#### Task 3.6: Update API Client to Use NextAuth Token
- [ ] Modify `lib/api/client.ts`:
  - Remove localStorage token retrieval
  - Use `getSession()` from next-auth/react
  - Inject `session.accessToken` as Bearer token
  - Handle 401 responses with automatic redirect to login

**File**: `/Users/foodtech/Documents/booker/booker-client/src/lib/api/client.ts`

**Sample Implementation**:
```typescript
import { getSession } from "next-auth/react";

export async function apiRequest<T>(
  endpoint: string,
  options?: RequestInit
): Promise<T> {
  const session = await getSession();

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(session?.accessToken && {
        Authorization: `Bearer ${session.accessToken}`,
      }),
      ...options?.headers,
    },
  });

  if (response.status === 401) {
    // Token expired, redirect to login
    window.location.href = "/api/auth/signin";
    throw new Error("Unauthorized");
  }

  return response.json();
}
```

**Reasoning**: Centralized token injection ensures all API calls are authenticated. 401 handling provides seamless re-authentication.

#### Task 3.7: Remove Old AuthContext
- [ ] Delete `lib/auth/AuthContext.tsx`
- [ ] Remove any imports of AuthProvider, useAuth, login, logout
- [ ] Update all components using useAuth to use useSession instead

**Files to Update**:
- `/Users/foodtech/Documents/booker/booker-client/src/lib/auth/AuthContext.tsx` (delete)
- Any components importing from this file

**Reasoning**: Eliminates duplicate auth logic and potential conflicts.

---

### Phase 4: Implement Token Refresh Strategy
**Goal**: Automatically refresh expired access tokens using refresh tokens

#### Task 4.1: Implement Token Refresh in NextAuth Callback
- [ ] Update JWT callback in `[...nextauth]/route.ts`:
  - Check if access token is expired
  - If expired, call Keycloak token endpoint with refresh token
  - Update token with new access_token and refresh_token
  - Handle refresh token expiration (force re-login)

**File**: `/Users/foodtech/Documents/booker/booker-client/src/app/api/auth/[...nextauth]/route.ts`

**Implementation**:
```typescript
async function refreshAccessToken(token: JWT) {
  try {
    const response = await fetch(
      `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`,
      {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
          client_id: process.env.KEYCLOAK_CLIENT_ID!,
          client_secret: process.env.KEYCLOAK_CLIENT_SECRET!,
          grant_type: "refresh_token",
          refresh_token: token.refreshToken as string,
        }),
      }
    );

    const refreshedTokens = await response.json();

    if (!response.ok) throw refreshedTokens;

    return {
      ...token,
      accessToken: refreshedTokens.access_token,
      expiresAt: Date.now() + refreshedTokens.expires_in * 1000,
      refreshToken: refreshedTokens.refresh_token ?? token.refreshToken,
    };
  } catch (error) {
    return { ...token, error: "RefreshAccessTokenError" };
  }
}

// In JWT callback:
async jwt({ token, account }) {
  // Initial sign in
  if (account) {
    return {
      accessToken: account.access_token,
      refreshToken: account.refresh_token,
      expiresAt: account.expires_at! * 1000,
      ...token,
    };
  }

  // Token still valid
  if (Date.now() < token.expiresAt) {
    return token;
  }

  // Token expired, refresh it
  return refreshAccessToken(token);
}
```

**Reasoning**: Automatic token refresh provides seamless user experience. Users stay logged in without manual re-authentication.

#### Task 4.2: Handle Refresh Token Errors in UI
- [ ] Update components to check for `session.error === "RefreshAccessTokenError"`
- [ ] Show "Session expired" message and redirect to login
- [ ] Optionally show notification before redirect

**Reasoning**: When refresh token expires (after 14 days), user must re-authenticate. Graceful error handling improves UX.

---

### Phase 5: Protected Routes and Auth Guards
**Goal**: Protect pages that require authentication

#### Task 5.1: Create Auth Guard Middleware
- [ ] Create `middleware.ts` in root of app directory:
  - Use NextAuth's `withAuth` middleware
  - Protect specific routes (/profile, /my-loans, etc.)
  - Redirect unauthenticated users to home page

**File**: `/Users/foodtech/Documents/booker/booker-client/src/middleware.ts`

**Implementation**:
```typescript
export { default } from "next-auth/middleware";

export const config = {
  matcher: ["/profile/:path*", "/my-loans/:path*", "/admin/:path*"],
};
```

**Reasoning**: Server-side route protection prevents unauthorized access even if client-side checks are bypassed.

#### Task 5.2: Create Client-Side Route Guards
- [ ] Create `components/auth/ProtectedRoute.tsx`:
  - Check session status
  - Show loading state while checking
  - Redirect to login if unauthenticated
  - Render children if authenticated

**File**: `/Users/foodtech/Documents/booker/booker-client/src/components/auth/ProtectedRoute.tsx`

**Reasoning**: Client-side guards provide immediate feedback and prevent rendering protected content during authentication check.

#### Task 5.3: Wrap Protected Pages
- [ ] Update pages that require auth:
  - `/app/profile/page.tsx`
  - `/app/my-loans/page.tsx`
  - Wrap with ProtectedRoute component or use middleware

**Reasoning**: Defense in depth - both server and client-side protection.

---

### Phase 6: UI/UX Enhancements
**Goal**: Polish the login experience to match the screenshot requirements

#### Task 6.1: Style Login Button to Match Screenshot
- [ ] Update Header Login button styling:
  - White background with rounded corners
  - "Login" text in dark color
  - Position in top-right corner
  - Add hover effects

**File**: `/Users/foodtech/Documents/booker/booker-client/src/components/layout/Header.tsx`

**Reference**: Screenshot shows white rounded button with "Login" text
**Reasoning**: Consistent with design mockup provided by user.

#### Task 6.2: Customize Keycloak Login Page (Optional)
- [ ] Create custom Keycloak theme if needed
- [ ] Configure "Sign in with Google" button styling
- [ ] Set application branding (logo, colors)

**Access**: Keycloak Admin → Realm Settings → Themes
**Reasoning**: Default Keycloak UI is functional but can be branded to match BOOKER application.

#### Task 6.3: Add Loading States
- [ ] Show loading spinner during OAuth redirect flow
- [ ] Show skeleton UI while checking session
- [ ] Add loading state to Login button

**Reasoning**: Prevents flash of unauthenticated content and improves perceived performance.

#### Task 6.4: Add User Avatar and Dropdown
- [ ] When logged in, show user's Google profile picture
- [ ] Create dropdown menu with:
  - User name and email
  - "My Profile" link
  - "My Loans" link
  - "Sign Out" button

**File**: `/Users/foodtech/Documents/booker/booker-client/src/components/layout/Header.tsx`

**Reasoning**: Standard authenticated user UI pattern. Profile picture from Google OAuth provides visual confirmation of logged-in state.

---

### Phase 7: Error Handling and Edge Cases
**Goal**: Handle authentication failures gracefully

#### Task 7.1: Implement OAuth Error Handling
- [ ] Handle OAuth errors in NextAuth:
  - User cancels Google login
  - Google account not authorized
  - Keycloak configuration errors
  - Network failures during OAuth flow
- [ ] Show user-friendly error messages

**File**: `/Users/foodtech/Documents/booker/booker-client/src/app/api/auth/[...nextauth]/route.ts`

**Reasoning**: OAuth flow can fail for many reasons. Clear error messages help users and developers debug issues.

#### Task 7.2: Add JWT Validation Error Handling in Backend
- [ ] Configure Spring Security to return proper error responses:
  - 401 for missing/invalid token
  - 403 for insufficient permissions
  - Include error details in response body
- [ ] Add exception handler for authentication errors

**File**: `/Users/foodtech/Documents/booker/booker-server/src/main/java/com/bookerapp/core/infrastructure/config/SecurityConfig.java`

**Reasoning**: Consistent error responses help frontend handle different failure scenarios appropriately.

#### Task 7.3: Implement Token Revocation on Logout
- [ ] Call Keycloak logout endpoint when user signs out
- [ ] Revoke refresh token to prevent reuse
- [ ] Clear NextAuth session

**Implementation**: Update signOut() call to include Keycloak logout
**Reasoning**: Proper token revocation prevents token reuse after logout, improving security.

---

### Phase 8: Testing and Verification
**Goal**: Ensure OAuth flow works end-to-end

#### Task 8.1: Manual Testing Checklist
- [ ] Test complete OAuth flow:
  1. Click Login button → redirected to Keycloak
  2. Click "Sign in with Google" → Google popup appears
  3. Select Google account → redirected back to app
  4. Verify user is logged in (name/avatar displayed)
  5. Verify API calls include Bearer token
  6. Test protected routes (should be accessible)
  7. Test logout → verify redirect to home
- [ ] Test token refresh:
  1. Wait for access token to expire (15 min)
  2. Make API call → should auto-refresh
  3. Verify new token is used
- [ ] Test session expiration:
  1. Wait for refresh token to expire (14 days - can shorten for testing)
  2. Make API call → should redirect to login
- [ ] Test error scenarios:
  1. Cancel Google login → verify error message
  2. Deny Google permissions → verify error handling
  3. Stop Keycloak → verify connection error message

#### Task 8.2: Backend API Testing
- [ ] Test JWT validation:
  1. Call API with valid Keycloak token → 200 OK
  2. Call API with no token → 401 Unauthorized
  3. Call API with expired token → 401 Unauthorized
  4. Call API with invalid signature → 401 Unauthorized
- [ ] Test role-based access (if implemented):
  1. Call admin endpoint with user role → 403 Forbidden
  2. Call admin endpoint with admin role → 200 OK
- [ ] Verify UserContext extraction:
  1. Add log in controller to print UserContext
  2. Verify userId, email, username are correct

#### Task 8.3: CORS Testing
- [ ] Verify Next.js (localhost:3000) can call API (localhost:8084)
- [ ] Check browser console for CORS errors
- [ ] Test preflight OPTIONS requests

---

## Technical Decisions and Reasoning

### Why Keycloak Instead of Direct Google OAuth?
1. **Centralized Auth**: Single source of truth for all authentication
2. **Provider Flexibility**: Easy to add GitHub, Apple, or custom auth later
3. **Consistent Tokens**: All providers issue same JWT format
4. **User Management**: Built-in user database and admin UI
5. **Enterprise Features**: SSO, user federation, identity brokering

### Why NextAuth.js Instead of Custom Implementation?
1. **Security**: Battle-tested OAuth implementation
2. **Token Refresh**: Automatic refresh token rotation
3. **Session Management**: Secure session handling
4. **Provider Support**: 50+ built-in OAuth providers
5. **Developer Experience**: Simple API, well-documented

### Why JWT Strategy Over Database Sessions?
1. **Stateless Backend**: No session storage needed in Spring Boot
2. **Scalability**: No shared session store required
3. **Performance**: No database lookup per request
4. **Simplicity**: Fewer moving parts

### Token Storage Decision: Session vs Local Storage
**Decision**: Use NextAuth.js session (server-side)
**Reasoning**:
- ❌ Local Storage: Vulnerable to XSS attacks
- ✅ Session: Tokens never exposed to client JavaScript
- ✅ HttpOnly Cookies: Protected from XSS
- NextAuth.js handles secure token storage automatically

---

## Security Considerations

### 1. Token Lifespans
- Access Token: 15 minutes (short to limit exposure)
- Refresh Token: 14 days (balance between security and UX)
- Session Idle: 30 minutes (auto-logout inactive users)

### 2. CORS Configuration
- Allow only `http://localhost:3000` in development
- Update to production domain when deployed
- Never use `*` wildcard in production

### 3. Environment Variables
- Never commit secrets to git
- Use different secrets for dev/staging/prod
- Rotate secrets regularly
- Consider using secret manager (AWS Secrets Manager, etc.)

### 4. HTTPS in Production
- Keycloak must use HTTPS in production
- OAuth redirect URIs must be HTTPS
- Cookies should have `Secure` flag
- HSTS headers should be enabled

### 5. Token Revocation
- Implement proper logout that revokes tokens
- Consider token blacklist for immediate revocation
- Monitor for unusual token usage patterns

---

## Rollback Plan

If OAuth implementation causes issues:

### Immediate Rollback (Keep Mock Auth)
1. Don't delete old AuthContext until OAuth is verified working
2. Keep feature flag to switch between mock and OAuth
3. Revert by removing NextAuth and re-enabling AuthContext

### Gradual Migration
1. Deploy OAuth to staging first
2. Test thoroughly before production
3. Keep mock auth as fallback for 1-2 sprints
4. Monitor error rates after OAuth deployment

---

## Dependencies and Prerequisites

### Required Before Starting
1. ✅ Google Cloud Console account with OAuth credentials
2. ✅ Keycloak instance running (docker-compose)
3. ✅ PostgreSQL database for Keycloak
4. ✅ Valid domain names (even localhost) registered in configs

### External Services
1. **Google OAuth**: Requires Google Cloud project
2. **Keycloak**: Requires persistent database
3. **No additional costs**: All services free for development

---

## Estimated Implementation Time

Based on MVP (Minimum Viable Product) approach:

| Phase | Task | Estimated Time |
|-------|------|----------------|
| 1 | Keycloak Setup | 2-3 hours |
| 2 | Backend Config | 2-3 hours |
| 3 | Frontend OAuth | 3-4 hours |
| 4 | Token Refresh | 1-2 hours |
| 5 | Protected Routes | 1-2 hours |
| 6 | UI/UX Polish | 2-3 hours |
| 7 | Error Handling | 1-2 hours |
| 8 | Testing | 2-3 hours |
| **Total** | | **14-22 hours** |

**Note**: This assumes no major blockers. Add buffer time for:
- Learning curve if unfamiliar with technologies
- Debugging OAuth flow issues
- Network/environment setup issues

---

## Success Criteria

Implementation is complete when:

1. ✅ User clicks "Login" button
2. ✅ Google OAuth popup appears
3. ✅ User selects Google account and grants permission
4. ✅ User is redirected back to BOOKER with logged-in state
5. ✅ User's name and avatar appear in header
6. ✅ API calls include valid JWT Bearer token
7. ✅ Spring Boot validates token and extracts user info
8. ✅ Protected routes only accessible when logged in
9. ✅ Token automatically refreshes when expired
10. ✅ User can logout and token is revoked
11. ✅ Error scenarios are handled gracefully

---

## Future Enhancements (Post-MVP)

Once basic OAuth is working:

1. **Additional Providers**: GitHub, Apple, email/password
2. **Role Management**: Admin panel for user roles
3. **User Profile**: Edit profile, change preferences
4. **2FA**: Two-factor authentication support
5. **Social Features**: Link multiple OAuth accounts
6. **Analytics**: Login metrics, user engagement
7. **Mobile**: OAuth flow for mobile apps (PKCE flow)

---

## File Structure After Implementation

```
booker-client/
├── src/
│   ├── app/
│   │   ├── api/
│   │   │   └── auth/
│   │   │       └── [...nextauth]/
│   │   │           └── route.ts          # ✨ NEW: NextAuth config
│   │   ├── layout.tsx                    # Update: Add SessionProvider
│   │   └── providers.tsx                 # Update: Replace AuthProvider
│   ├── components/
│   │   ├── auth/
│   │   │   ├── ProtectedRoute.tsx        # ✨ NEW: Route guard
│   │   │   └── UserMenu.tsx              # ✨ NEW: Avatar dropdown
│   │   └── layout/
│   │       └── Header.tsx                # Update: OAuth login button
│   ├── lib/
│   │   ├── api/
│   │   │   └── client.ts                 # Update: Use NextAuth token
│   │   └── auth/
│   │       └── AuthContext.tsx           # ❌ DELETE: Replaced by NextAuth
│   └── middleware.ts                     # ✨ NEW: Route protection
├── .env.local                            # ✨ NEW: Keycloak credentials
└── package.json                          # Update: Add next-auth

booker-server/
└── src/main/java/com/bookerapp/core/
    ├── infrastructure/
    │   ├── config/
    │   │   └── SecurityConfig.java       # ✨ NEW: Spring Security OAuth2
    │   └── security/
    │       └── KeycloakJwtAuthenticationConverter.java  # ✨ NEW
    ├── presentation/
    │   ├── interceptor/
    │   │   └── JwtAuthInterceptor.java   # ❌ DELETE or simplify
    │   └── argumentresolver/
    │       └── UserContextArgumentResolver.java  # Update: Use SecurityContext
    └── resources/
        └── application.yml               # Update: Add OAuth2 config

docker-compose.yml                        # Update: Add Keycloak + PostgreSQL
```

---

## References and Resources

### Official Documentation
- [Google OAuth 2.0](https://developers.google.com/identity/protocols/oauth2)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [NextAuth.js](https://next-auth.js.org/)
- [Spring Security OAuth 2.0 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)

### Key Concepts
- OAuth 2.0 Authorization Code Flow
- PKCE (Proof Key for Code Exchange)
- JWT (JSON Web Tokens)
- JWKS (JSON Web Key Set)
- OpenID Connect (OIDC)

---

## Questions to Clarify Before Implementation

1. **Keycloak Database**: Should we use PostgreSQL or H2 for Keycloak? (Recommendation: PostgreSQL for persistence)

2. **Realm Name**: Use existing "myrealm" or create new "booker" realm?

3. **User Data Sync**: Should we sync Google user data to BOOKER database or rely only on JWT claims?

4. **Role Assignment**: How should roles be assigned to new users? (Default: USER role)

5. **Production Domains**: What are the production URLs for Next.js and Spring Boot? (Needed for OAuth redirect URIs)

6. **Email Verification**: Should we require email verification for Google OAuth users? (Recommendation: Trust Google's email verification)

7. **Session Timeout**: Is 30 minutes idle timeout acceptable or should it be different?

8. **Multiple OAuth Providers**: Do you want to support other providers (GitHub, Apple) now or later? (Recommendation: Start with Google only)

---

## Approval Required

**Status**: ⏳ Awaiting Review

Please review this plan and provide feedback on:
1. Overall approach (Keycloak + NextAuth.js + Spring Security)
2. Estimated timeline and MVP scope
3. Security considerations
4. Any additional requirements or constraints
5. Answers to the questions above

Once approved, I will begin implementation following this plan and updating progress in this document.

---

**Document Version**: 1.0
**Last Updated**: 2025-12-16
**Next Review**: After approval

/**
 * REST boundary for authentication and authorization flows.
 *
 * <p>Auth is intentionally separated from user profile management: Keycloak,
 * OAuth2/OIDC, JWT validation and token lifecycle belong here, while user
 * business data belongs to the user domain.</p>
 */
package museon_online.astor_butler.api.auth;

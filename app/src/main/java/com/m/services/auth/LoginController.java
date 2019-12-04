package com.m.services.auth;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.m.helper.MobileService;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import static com.m.services.auth.LoginActivity.RE_AUTH;
import static com.m.services.auth.LoginActivity.mAuthStateManager;

public class LoginController {
    private Context context;
    private MobileService mobileService;
    private AuthorizationService mAuthService;
    private LoginListener listener;
    public static AuthState mAuthState;


    public LoginController(Context context){
        this.context = context;
        mobileService = MobileService.getInstance();

    }

    public LoginController(Context context, LoginListener listener, MobileService mobileService){
        this.context = context;
        this.listener = listener;
        this.mobileService = mobileService;
    }

    public void reAuthorise() {
        Intent redirectToRefreshToken = new Intent(context, LoginActivity.class);
        context.startActivity(redirectToRefreshToken);
    }

    public void doAuth() {
        mAuthService = new AuthorizationService(context);
        final AuthorizationServiceConfiguration.RetrieveConfigurationCallback retrieveCallback =
                (authorizationServiceConfiguration, e) -> {
                    if (e != null) {
                        System.out.println("Failed to retrieve configuration for " + mobileService.getKIssuer());
                    } else {
                        makeAuthRequest(authorizationServiceConfiguration);
                    }
                };

        String discoveryEndpoint = mobileService.getKIssuer() + ".well-known/openid-configuration";
        AuthorizationServiceConfiguration.fetchFromUrl(Uri.parse(discoveryEndpoint), retrieveCallback);
    }

    public void makeAuthRequest(AuthorizationServiceConfiguration serviceConfiguration) {
        String k_REDIRECT_URI = "com.m.androidnativeapp:/oauth2redirect";
        AuthorizationRequest.Builder authorizationRequest = new AuthorizationRequest.Builder(
                serviceConfiguration,
                mobileService.getKClientId(),
                ResponseTypeValues.CODE,
                Uri.parse(k_REDIRECT_URI));
        AuthorizationRequest authRequest = authorizationRequest.build();
        Intent authIntent = mAuthService.getAuthorizationRequestIntent(authRequest);
        listener.reAuth(authIntent);
    }

    public void handleAuthLoginResponse(Intent data){
        AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
        AuthorizationException ex = AuthorizationException.fromIntent(data);

        mAuthStateManager.updateAfterAuthorization(resp, ex);
        mAuthState = new AuthState(resp, ex);
        exchangeAuthorizationCode(resp);
    }

    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        performTokenRequest(authorizationResponse.createTokenExchangeRequest());
    }

    private void performTokenRequest(TokenRequest request) {
        mAuthService.performTokenRequest(
                request,
                (tokenResponse, ex) -> {
                    receivedTokenResponse(tokenResponse, ex);
                    mAuthStateManager.updateAfterTokenResponse(tokenResponse, ex);
                });
    }

    private void receivedTokenResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
            mAuthState.update(tokenResponse, authException);
            listener.authComplete();

    }
}

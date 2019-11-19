package com.m.services.Auth;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.m.androidNativeApp.MainActivity;
import com.m.helper.MobileService;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

public class LoginActivity extends AppCompatActivity {

    public static AuthState mAuthState;
    public static AuthStateManager mAuthStateManager;
    public static MobileService mobileService;
    private AuthorizationService mAuthService;
    private int RC_AUTH = 100;
    public static int RE_AUTH = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuthStateManager = AuthStateManager.getInstance(this);
        Context context = getApplicationContext();
        mobileService = MobileService.getInstance(context.getApplicationContext());

        if (mAuthStateManager.getCurrent().isAuthorized() && RE_AUTH == 0) {
            System.out.println("User is authorized already");
            startActivity(new Intent(this, MainActivity.class));

            finish();

        } else {
            doAuth();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_AUTH) {
            AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
            AuthorizationException ex = AuthorizationException.fromIntent(data);

            mAuthStateManager.updateAfterAuthorization(resp, ex);
            mAuthState = new AuthState(resp, ex);
            exchangeAuthorizationCode(resp);

        } else {
            System.out.println("Error, wrong response code");
        }
    }

    public void doAuth() {
        mAuthService = new AuthorizationService(this);
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
        startActivityForResult(authIntent, RC_AUTH);

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

        System.out.println("Token expires in : " + mAuthState.getAccessTokenExpirationTime());
        startActivity(new Intent(this, MainActivity.class));
    }
}
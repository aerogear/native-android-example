package com.m.services.appAuth;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.m.services.appAuth.AuthStateManager;
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

public class AppAuthActivity extends AppCompatActivity {

    public static AuthState mAuthState;
    public static AuthStateManager mAuthStateManager;
    public static MobileService mobileService;
    private AuthorizationService mAuthService;
    private int RC_AUTH = 100;
    public static int RE_AUTH = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();

        mAuthStateManager = AuthStateManager.getInstance(this);
        mobileService = MobileService.getInstance(context.getApplicationContext());


        /**
         * Check if user is currently authorized, if user token expires RE_AUTH value is updated
         * and user is redirected to login screen
         */
        if (mAuthStateManager.getCurrent().isAuthorized() && RE_AUTH == 0) {
            startActivity(new Intent(this, MainActivity.class));

            finish();

        } else {

            doAuth();
        }
    }


    /**
     Fetching well-known configuration for given KIssuer, once configuration is fetched
     authorization request is initialized. KIssuer is provided in mobile-services.json file
     in ../../../assets folder.
     */
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


    /**
     * Once authorization configuration has been returned successfully in  authorization request is
     * made with given service configuration, clientId provided in mobile-services.json file and
     * redirectUri which can be changed within below method. App redirects user to login page and
     * after successful login user is redirected back to app and onActivityResult is initiated.
     * @param serviceConfiguration
     *          Instructions to the app on how to interact with the authorization service.
     */
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


    /**
     * Once user successfully logs in authorization response is returned and used to exchange
     * authorization code.
     * @param requestCode
     *          A status code for assumed response received
     * @param resultCode
     *          Actual response code from authorization service
     * @param data
     *          Data received from authorization request
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_AUTH) {
            AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
            AuthorizationException ex = AuthorizationException.fromIntent(data);

            System.out.println("RESP IS : "+ resp);
            mAuthStateManager.updateAfterAuthorization(resp, ex);
            mAuthState = new AuthState(resp, ex);
            exchangeAuthorizationCode(resp);

        } else {
            System.out.println("Error, wrong response code");
        }
    }


    /**
     * Exchanging authorization code to perform token request
     * @param authorizationResponse
     *            Data received from onActivityResult
     */
    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        performTokenRequest(authorizationResponse.createTokenExchangeRequest());
    }


    /**
     * Sending a request for a token
     * @param request
     *          Using response received to pass in token exchange request
     */
    private void performTokenRequest(TokenRequest request) {
        mAuthService.performTokenRequest(
                request,
                (tokenResponse, ex) -> {
                    receivedTokenResponse(tokenResponse, ex);
                    mAuthStateManager.updateAfterTokenResponse(tokenResponse, ex);
                });
    }

    /**
     * Updating auth state with the token so the token can be read and passed around the app.
     * @param tokenResponse
     *          Token received
     * @param authException
     *          Exception in case of failure
     */
    private void receivedTokenResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        mAuthState.update(tokenResponse, authException);

        /**
         * An example of what can be done with received token saved to the auth state.
         */
        System.out.println("Token expires in : " + mAuthState.getAccessTokenExpirationTime());
        startActivity(new Intent(this, MainActivity.class));
    }
}
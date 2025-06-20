package com.sigma.openfashion.ui.splash;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.sigma.openfashion.R;
import com.sigma.openfashion.SharedPrefHelper;
import com.sigma.openfashion.data.SupabaseService;
import com.sigma.openfashion.ui.BaseFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * SplashFragment — загрузочный экран с анимацией и проверкой:
 * 1. сеть
 * 2. сервер Supabase
 * 3. загрузка начальных данных (категорий)
 * 4. сессия (JWT + PIN или Onboarding)
 */
public class SplashFragment extends BaseFragment {

    private static final long LOGO_ANIM_DURATION = 600; // мс
    private static final long NEXT_STEP_DELAY     = 300; // мс

    private View splashLogo;
    private ProgressBar splashProgress;
    private TextView statusText;
    private Button actionButton;

    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;

    public SplashFragment() {
        super(R.layout.fragment_splash);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        splashLogo     = view.findViewById(R.id.splashLogo);
        splashProgress = view.findViewById(R.id.splashProgress);
        statusText     = view.findViewById(R.id.statusText);
        actionButton   = view.findViewById(R.id.actionButton);

        supabaseService = new SupabaseService();
        prefs           = SharedPrefHelper.getInstance(requireContext());

        // Скрываем всё, кроме логотипа
        splashLogo.setVisibility(View.GONE);
        splashProgress.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);
        actionButton.setVisibility(View.GONE);

        animateLogo();
    }

    /** Анимация логотипа (масштаб + плавное появление) */
    private void animateLogo() {
        runOnUi(() -> splashLogo.setVisibility(View.VISIBLE));

        ScaleAnimation scaleAnim = new ScaleAnimation(
                0.5f, 1f, 0.5f, 1f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnim.setDuration(LOGO_ANIM_DURATION);
        runOnUi(() -> splashLogo.startAnimation(scaleAnim));

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(LOGO_ANIM_DURATION);
        runOnUi(() -> splashLogo.startAnimation(fadeIn));

        new Handler(Looper.getMainLooper())
                .postDelayed(this::startInitialization, LOGO_ANIM_DURATION + NEXT_STEP_DELAY);
    }

    /** Этап 1: Проверка сети */
    private void startInitialization() {
        runOnUi(() -> {
            splashProgress.setVisibility(View.VISIBLE);
            statusText.setVisibility(View.VISIBLE);
            setStatus(getString(R.string.Network_check));
        });

        if (!SupabaseService.isNetworkAvailable(requireContext())) {
            runOnUi(() -> {
                setStatus(getString(R.string.No_internet));
                showRetryButton();
            });
        } else {
            checkServer();
        }
    }

    /** Этап 2: Проверка доступности Supabase */
    private void checkServer() {
        runOnUi(() -> setStatus(getString(R.string.Checking_server_availability)));

        supabaseService.checkServerConnection(new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                new Handler(Looper.getMainLooper())
                        .postDelayed(SplashFragment.this::checkSession, NEXT_STEP_DELAY);
            }

            @Override
            public void onError(String errorMessage) {
                runOnUi(() -> {
                    setStatus(getString(R.string.Server_unavailable));
                    showRetryButton();
                });
            }
        });
    }

    /** Этап 3: Проверка сессии (JWT + PIN или Onboarding) */
    private void checkSession() {
        runOnUi(() -> setStatus(getString(R.string.Checking_in_on_the_session)));

        String jwt             = prefs.getJwtToken();
        String refreshToken    = prefs.getRefreshToken();
        String userId          = prefs.getUserId();

        if (jwt == null || jwt.isEmpty() || userId == null || userId.isEmpty()) {
            // Нет JWT/профиля → идём на Onboarding или сразу на Auth
            runOnUi(this::navigateToOnboarding);
        } else {
            // Есть JWT, проверяем профиль, затем PIN
            supabaseService.setJwtToken(jwt);
            supabaseService.signIn( new SupabaseService.QueryCallback() {
                @Override
                public void onSuccess(String jsonResponse) {
                    nextTo();
                }
                @Override
                public void onError(String errorMessage) {
                    if (refreshToken == null || refreshToken.trim().isEmpty()){
                        runOnUi(() -> navigateToAuthWithMessage(getString(R.string.Session_error)));
                    } else {
                        runOnUi(()-> supabaseService.refreshSession(refreshToken, new SupabaseService.QueryCallback() {
                            @Override
                            public void onSuccess(String jsonResponse) {
                                try {
                                    JSONObject obj       = new JSONObject(jsonResponse);
                                    String accessToken   = obj.optString("access_token", null);
                                    String refresh_token = obj.optString("refresh_token", null);

                                    prefs.saveJwtToken(accessToken);
                                    prefs.saveRefreshToken(refresh_token);
                                    nextTo();
                                } catch (JSONException e) {
                                    setStatus(getString(R.string.Parse_Error) + e.getMessage());
                                    showRetryButton();
                                }
                            }
                            @Override
                            public void onError(String errorMessage) {
                                prefs.saveJwtToken(null);
                                prefs.saveRefreshToken(null);
                                runOnUi(() -> navigateToAuthWithMessage(getString(R.string.Token_update_error)));
                            }
                        }));
                    }
                }
            });
        }
    }

    private void nextTo() {
        runOnUi(() -> {
            String userId = prefs.getUserId();
            supabaseService.getProfile(userId, new SupabaseService.QueryCallback() {
                @Override
                public void onSuccess(String jsonResponse) {
                    if (jsonResponse.trim().equals("[]")) {
                        runOnUi(() -> navigateToRegWithMessage());
                    } else {
                        try {
                            JSONArray array = new JSONArray(jsonResponse);
                            JSONObject obj = array.getJSONObject(0);
                            String first_name   = obj.optString("first_name", null);
                            String last_name = obj.optString("last_name", null);

                            prefs.saveKeyUserFirstName(first_name);
                            prefs.saveKeyUserLastName(last_name);
                            runOnUi(() -> navigateToPinEntry());
                        } catch (JSONException e) {
                            setStatus(getString(R.string.Parse_Error) + e.getMessage());
                            showRetryButton();
                        }
                    }
                }
                @Override
                public void onError(String errorMessage) {
                    runOnUi(() -> navigateToAuthWithMessage(getString(R.string.Session_error)));
                }
            });
        });
    }

    /** Показывает кнопку “Повторить” при ошибке */
    private void showRetryButton() {
        runOnUi(() -> {
            splashProgress.setVisibility(View.GONE);
            statusText.setVisibility(View.VISIBLE);
            actionButton.setVisibility(View.VISIBLE);
            actionButton.setEnabled(true);
            actionButton.setText(getString(R.string.Repeat));
            actionButton.setOnClickListener(v -> {
                actionButton.setVisibility(View.GONE);
                splashProgress.setVisibility(View.VISIBLE);
                setStatus(getString(R.string.Re_checking));
                startInitialization();
            });
        });
    }

    /** Переход на экран Auth (с сообщением) */
    private void navigateToAuthWithMessage(String message) {
        runOnUi(() -> {
            setStatus(message);
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    NavHostFragment.findNavController(SplashFragment.this)
                            .navigate(R.id.action_splash_to_auth), NEXT_STEP_DELAY);
        });
    }

    /** Переход на экран Reg (с сообщением) */
    private void navigateToRegWithMessage() {
        runOnUi(() -> {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    NavHostFragment.findNavController(SplashFragment.this)
                            .navigate(R.id.action_splash_to_reg), NEXT_STEP_DELAY);
        });
    }

    /** Переход на экран Onboarding */
    private void navigateToOnboarding() {
        NavHostFragment.findNavController(SplashFragment.this)
                .navigate(R.id.action_splash_to_onboarding);
    }

    /** Переход на экран ввода/установки PIN */
    private void navigateToPinEntry() {
        NavHostFragment.findNavController(SplashFragment.this)
                .navigate(R.id.action_splash_to_pin);
    }

    /** Установить текст статуса */
    private void setStatus(String text) {
        statusText.setText(text);
    }
}

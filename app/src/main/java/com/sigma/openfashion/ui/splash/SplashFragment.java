package com.sigma.openfashion.ui.splash;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

/**
 * SplashFragment — загрузочный экран с анимацией и проверкой:
 * 1. сеть
 * 2. сервер Supabase
 * 3. загрузка начальных данных (категорий)
 * 4. сессия (JWT + PIN или Onboarding)
 */
public class SplashFragment extends Fragment {

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
            setStatus("Проверка сети…");
        });

        if (!SupabaseService.isNetworkAvailable(requireContext())) {
            runOnUi(() -> {
                setStatus("Нет интернета");
                showRetryButton();
            });
        } else {
            checkServer();
        }
    }

    /** Этап 2: Проверка доступности Supabase */
    private void checkServer() {
        runOnUi(() -> setStatus("Проверка доступности сервера…"));

        supabaseService.checkServerConnection(new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                new Handler(Looper.getMainLooper())
                        .postDelayed(SplashFragment.this::loadCategories, NEXT_STEP_DELAY);
            }

            @Override
            public void onError(String errorMessage) {
                runOnUi(() -> {
                    setStatus("Сервер недоступен");
                    showRetryButton();
                });
            }
        });
    }

    /** Этап 3: Загрузка категорий (или других стартовых данных) */
    private void loadCategories() {
        runOnUi(() -> setStatus("Загрузка данных…"));

        supabaseService.getCategories(new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                new Handler(Looper.getMainLooper())
                        .postDelayed(SplashFragment.this::checkSession, NEXT_STEP_DELAY);
            }

            @Override
            public void onError(String errorMessage) {
                runOnUi(() -> {
                    setStatus("Ошибка загрузки данных");
                    showRetryButton();
                });
            }
        });
    }

    /** Этап 4: Проверка сессии (JWT + PIN или Onboarding) */
    private void checkSession() {
        runOnUi(() -> setStatus("Проверка сессии…"));

        String jwt    = prefs.getJwtToken();
        String userId = prefs.getUserId();

        if (jwt == null || jwt.isEmpty() || userId == null || userId.isEmpty()) {
            // Нет JWT/профиля → идём на Onboarding или сразу на Auth
            runOnUi(() -> navigateToOnboarding());
        } else {
            // Есть JWT, проверяем профиль, затем PIN
            supabaseService.setJwtToken(jwt);
            supabaseService.getProfile(userId, new SupabaseService.QueryCallback() {
                @Override
                public void onSuccess(String jsonResponse) {
                    Log.d("SplashFragment", jsonResponse);
                    // Если профиль пустой, считаем, что сессия истекла
                    if (jsonResponse.trim().equals("[]")) {
                        runOnUi(() -> navigateToAuthWithMessage("Сессия устарела"));
                    } else {
                        runOnUi(() -> navigateToPinEntry());
                    }
                }
                @Override
                public void onError(String errorMessage) {
                    runOnUi(() -> navigateToAuthWithMessage("Ошибка сессии"));
                }
            });
        }
    }

    /** Показывает кнопку “Повторить” при ошибке */
    private void showRetryButton() {
        runOnUi(() -> {
            splashProgress.setVisibility(View.GONE);
            statusText.setVisibility(View.VISIBLE);
            actionButton.setVisibility(View.VISIBLE);
            actionButton.setEnabled(true);
            actionButton.setText("Повторить");
            actionButton.setOnClickListener(v -> {
                actionButton.setVisibility(View.GONE);
                splashProgress.setVisibility(View.VISIBLE);
                setStatus("Повторная проверка…");
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

    /** Безопасный вызов UI‑операции из любого потока */
    private void runOnUi(Runnable block) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(block);
        }
    }
}

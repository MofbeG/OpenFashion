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

/**
 * SplashFragment — доработанный загрузочный экран с анимацией и безопасными UI‑вызовами.
 */
public class SplashFragment extends Fragment {

    private static final long LOGO_ANIM_DURATION = 600; // длительность анимации (мс)
    private static final long NEXT_STEP_DELAY     = 300; // задержка между этапами (мс)

    private View splashLogo;
    private ProgressBar splashProgress;
    private TextView statusText;
    private Button actionButton;

    private SupabaseService supabaseService;
    private String userId; // user_id из SharedPrefs

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
        userId = SharedPrefHelper.getInstance(requireContext()).getUserId();

        // Сразу скрываем всё, кроме логотипа
        splashLogo.setVisibility(View.GONE);
        splashProgress.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);
        actionButton.setVisibility(View.GONE);

        animateLogo();
    }

    /** Анимация логотипа (scale + fadeIn) */
    private void animateLogo() {
        runOnUi(() -> splashLogo.setVisibility(View.VISIBLE));

        ScaleAnimation scaleAnim = new ScaleAnimation(
                0.5f, 1.0f,
                0.5f, 1.0f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnim.setDuration(LOGO_ANIM_DURATION);
        runOnUi(() -> splashLogo.startAnimation(scaleAnim));

        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(LOGO_ANIM_DURATION);
        runOnUi(() -> splashLogo.startAnimation(fadeIn));

        new Handler(Looper.getMainLooper())
                .postDelayed(this::startInitialization, LOGO_ANIM_DURATION + NEXT_STEP_DELAY);
    }

    /** Запуск этапов инициализации */
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

    /** Проверка доступности Supabase */
    private void checkServer() {
        runOnUi(() -> setStatus("Проверка сервера…"));

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

    /** Загрузка категорий */
    private void loadCategories() {
        runOnUi(() -> setStatus("Загрузка категорий…"));

        supabaseService.getCategories(new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                // Кэширование категории по желанию
                new Handler(Looper.getMainLooper())
                        .postDelayed(SplashFragment.this::checkSession, NEXT_STEP_DELAY);
            }

            @Override
            public void onError(String errorMessage) {
                runOnUi(() -> {
                    setStatus("Ошибка загрузки категорий");
                    showRetryButton();
                });
            }
        });
    }

    /** Проверка JWT и сессии */
    private void checkSession() {
        runOnUi(() -> setStatus("Проверка сессии…"));

        String jwt = SharedPrefHelper.getInstance(requireContext()).getJwtToken();
        if (jwt == null || jwt.isEmpty()) {
            runOnUi(this::showLoginButton);
        } else {
            supabaseService.setJwtToken(jwt);
            if (userId == null || userId.isEmpty()) {
                runOnUi(() -> needLogin("Требуется вход"));
            } else {
                supabaseService.getProfile(userId, new SupabaseService.QueryCallback() {
                    @Override
                    public void onSuccess(String jsonResponse) {
                        if (jsonResponse.trim().equals("[]")) {
                            runOnUi(() -> needLogin("Сессия истекла"));
                        } else {
                            new Handler(Looper.getMainLooper()).postDelayed(() ->
                                            NavHostFragment.findNavController(SplashFragment.this)
                                                    .navigate(R.id.action_splash_to_home),
                                    NEXT_STEP_DELAY
                            );
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUi(() -> needLogin("Ошибка сессии"));
                    }
                });
            }
        }
    }

    /** Показать кнопку «Повторить» */
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

    /** Показать кнопку «Войти» */
    private void showLoginButton() {
        runOnUi(() -> {
            splashProgress.setVisibility(View.GONE);
            statusText.setVisibility(View.VISIBLE);
            actionButton.setVisibility(View.VISIBLE);
            actionButton.setEnabled(true);
            actionButton.setText("Войти");
            actionButton.setOnClickListener(v ->
                    NavHostFragment.findNavController(SplashFragment.this)
                            .navigate(R.id.action_splash_to_auth)
            );
        });
    }

    /** Перейти к Auth с сообщением */
    private void needLogin(String message) {
        runOnUi(() -> setStatus(message));
        new Handler(Looper.getMainLooper())
                .postDelayed(() -> runOnUi(this::showLoginButton), NEXT_STEP_DELAY);
    }

    /** Простой вызов UI‑операции в главном потоке */
    private void runOnUi(Runnable block) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(block);
        }
    }

    /** Установить текст статуса */
    private void setStatus(String text) {
        statusText.setText(text);
    }
}

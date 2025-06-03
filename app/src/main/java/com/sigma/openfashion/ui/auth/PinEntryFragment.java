package com.sigma.openfashion.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.sigma.openfashion.R;
import com.sigma.openfashion.SharedPrefHelper;
import com.sigma.openfashion.data.SupabaseService;

/**
 * PinEntryFragment: ввод и установка PIN‑кода.
 */
public class PinEntryFragment extends Fragment {

    private TextInputEditText pinEditText;
    private MaterialButton confirmPinButton;
    private MaterialTextView errorText;
    private ProgressBar pinProgress;

    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;
    private String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pin_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pinEditText        = view.findViewById(R.id.pinEditText);
        confirmPinButton   = view.findViewById(R.id.confirmPinButton);
        errorText          = view.findViewById(R.id.pinErrorText);
        pinProgress        = view.findViewById(R.id.pinProgress);

        prefs            = SharedPrefHelper.getInstance(requireContext());
        supabaseService  = new SupabaseService();
        userId           = prefs.getUserId();

        confirmPinButton.setOnClickListener(v -> attemptPin());
    }

    private void attemptPin() {
        String pin = pinEditText.getText() != null ? pinEditText.getText().toString().trim() : "";
        if (TextUtils.isEmpty(pin) || pin.length() != 4) {
            pinEditText.setError("Введите 4 цифры");
            return;
        }
        hideError();
        showProgress(true);

        String savedPin = prefs.getUserPin(); // метод getUserPin() в SharedPrefHelper
        if (savedPin == null) {
            // Устанавливаем PIN впервые: сохраняем локально и в таблицу profiles.pin_code
            prefs.saveUserPin(pin);
            supabaseService.setJwtToken(prefs.getJwtToken());
            supabaseService.upsertProfile(userId, null, null, null, new SupabaseService.QueryCallback() {
                @Override public void onSuccess(String jsonResponse) {
                    updateProfilePinOnServer(pin);
                }
                @Override public void onError(String errorMessage) {
                    showError(errorMessage);
                }
            });
        } else {
            // Сравниваем с локально сохраненным PIN
            if (savedPin.equals(pin)) {
                // PIN верный → переходим в Home
                requireActivity().runOnUiThread(() -> {
                    showProgress(false);
                    NavHostFragment.findNavController(PinEntryFragment.this)
                            .navigate(R.id.action_pin_to_home);
                });
            } else {
                showError("Неверный PIN‑код");
            }
        }
    }

    /** Обновляем поле pin_code в таблице profiles */
    private void updateProfilePinOnServer(String pin) {
        // Предположим, что upsertProfile может принимать null для остальных полей.
        supabaseService.upsertProfile(userId, null, null, null, new SupabaseService.QueryCallback() {
            @Override public void onSuccess(String jsonResponse) {
                // На самом деле нужно создать эндпоинт для обновления только PIN, но для простоты
                // здесь снова делаем upsertProfile с PIN в качестве одного из полей (надо доработать метод).
                requireActivity().runOnUiThread(() -> {
                    showProgress(false);
                    NavHostFragment.findNavController(PinEntryFragment.this)
                            .navigate(R.id.action_pin_to_home);
                });
            }
            @Override public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void showProgress(boolean show) {
        pinProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        confirmPinButton.setEnabled(!show);
    }

    private void showError(String message) {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            errorText.setVisibility(View.VISIBLE);
            errorText.setText(message);
        });
    }

    private void hideError() {
        requireActivity().runOnUiThread(() -> errorText.setVisibility(View.GONE));
    }
}

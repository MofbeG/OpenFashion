package com.sigma.openfashion.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private TextView pinTitle;
    private TextInputEditText pinEditText;
    private MaterialButton confirmPinButton;
    private MaterialTextView errorText, exitText;
    private ProgressBar pinProgress;

    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;
    private String tempPin;
    private int count;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pin_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pinTitle           = view.findViewById(R.id.pinTitle);
        pinEditText        = view.findViewById(R.id.pinEditText);
        confirmPinButton   = view.findViewById(R.id.confirmPinButton);
        errorText          = view.findViewById(R.id.pinErrorText);
        exitText           = view.findViewById(R.id.exitText);
        pinProgress        = view.findViewById(R.id.pinProgress);

        prefs            = SharedPrefHelper.getInstance(requireContext());
        supabaseService  = new SupabaseService();

        if (prefs.getUserPin() == null)
            pinTitle.setText("Установка PIN-кода");
        else
            exitText.setVisibility(View.VISIBLE);

        exitText.setOnClickListener(view1 -> exit());
        confirmPinButton.setOnClickListener(v -> attemptPin());
    }

    private void attemptPin() {
        String pin = pinEditText.getText() != null ? pinEditText.getText().toString().trim() : "";
        if (TextUtils.isEmpty(pin) || pin.length() != 4) {
            pinEditText.setError("Введите 4 цифры");
            return;
        }
        pinEditText.setText("");
        hideError();
        showProgress(true);

        if (count >= 2) {
            exit();
        } else {
            String savedPin = prefs.getUserPin();
            if (savedPin == null) {
                showProgress(false);
                if (tempPin == null ||tempPin.trim().isEmpty()) {
                    tempPin = pin;
                    showError("Введите PIN-код ещё раз.");
                } else if (tempPin.equals(pin)) {
                    updateProfilePinOnServer(tempPin);
                } else {
                    tempPin = null;
                    showError("PIN-коды разные! Введите ещё раз.");
                }
            } else {
                if (savedPin.equals(pin)) {
                    requireActivity().runOnUiThread(() -> {
                        showProgress(false);
                        NavHostFragment.findNavController(PinEntryFragment.this)
                                .navigate(R.id.action_pin_to_home);
                    });
                } else {
                    count++;
                    showError("Неверный PIN‑код. Осталось попыток: " + count + " из 3");
                }
            }
        }
    }

    /** Обновляем поле pin_code в таблице profiles */
    private void updateProfilePinOnServer(String pin) {
        /*// Предположим, что upsertProfile может принимать null для остальных полей.
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
        });*/
        prefs.saveUserPin(pin);
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            NavHostFragment.findNavController(PinEntryFragment.this)
                    .navigate(R.id.action_pin_to_home);
        });
    }

    private void exit() {
        showProgress(true);
        supabaseService.setJwtToken(prefs.getJwtToken());
        supabaseService.logout(prefs.getRefreshToken(), new SupabaseService.QueryCallback() {
            @Override public void onSuccess(String jsonResponse) {
                requireActivity().runOnUiThread(() -> {
                    showProgress(false);
                    prefs.clearAll();
                    NavHostFragment.findNavController(PinEntryFragment.this)
                            .navigate(R.id.action_pin_to_auth);
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

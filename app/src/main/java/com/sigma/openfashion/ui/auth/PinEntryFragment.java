package com.sigma.openfashion.ui.auth;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.textview.MaterialTextView;
import com.sigma.openfashion.R;
import com.sigma.openfashion.SharedPrefHelper;
import com.sigma.openfashion.data.SupabaseService;
import com.sigma.openfashion.ui.BaseFragment;
import com.sigma.openfashion.ui.HeaderConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * PinEntryFragment: ввод и установка PIN‑кода.
 */
public class PinEntryFragment extends BaseFragment {
    private TextView pinTitle;
    private MaterialTextView errorText, exitText;
    private ProgressBar pinProgress;
    private List<TextView> pinViews = new ArrayList<>();
    private List<Integer> digits = new ArrayList<>();
    private List<Button> btnDigits = new ArrayList<>();
    private final int[] digitButtons = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
    private Vibrator vibrator;

    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;
    private String tempPin;
    private int count;

    public PinEntryFragment() {
        super(R.layout.fragment_pin_entry);
    }

    @Override
    public void onResume() {
        super.onResume();
        setHeaderVisibility(true);
        setupHeader(HeaderConfig.LOGO_ONLY);
    }

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
        errorText          = view.findViewById(R.id.pinErrorText);
        exitText           = view.findViewById(R.id.exitText);
        pinProgress        = view.findViewById(R.id.pinProgress);

        prefs            = SharedPrefHelper.getInstance(requireContext());
        supabaseService  = new SupabaseService();

        if (prefs.getUserPin() == null)
            pinTitle.setText(getString(R.string.Setting_the_PIN_code));
        else
            exitText.setVisibility(View.VISIBLE);


        vibrator = ContextCompat.getSystemService(view.getContext(), Vibrator.class);

        pinViews.add(view.findViewById(R.id.pinDigit1));
        pinViews.add(view.findViewById(R.id.pinDigit2));
        pinViews.add(view.findViewById(R.id.pinDigit3));
        pinViews.add(view.findViewById(R.id.pinDigit4));

        for (int i = 0; i < digitButtons.length; i++) {
            final int digit = i;
            Button btn = view.findViewById(digitButtons[i]);
            btnDigits.add(btn);
            btnDigits.get(i).setOnClickListener(v -> onDigitPressed(digit));
        }

        view.findViewById(R.id.deleteKey).setOnClickListener(v -> {
            vibrate(30);
            if (!digits.isEmpty()) {
                digits.remove(digits.size() - 1);
                updatePinView();
            }
        });

        exitText.setOnClickListener(view1 -> exit());
    }
    private void onDigitPressed(int d) {
        hideError();
        if (digits.size() < 4) {
            digits.add(d);
            updatePinView();
            vibrate(30);
            if (digits.size() == 4) {
                showProgress(true);

                String pin = "";
                for (int digit : digits) pin += digit;

                if (count >= 2) {
                    exit();
                } else {
                    String savedPin = prefs.getUserPin();
                    if (savedPin == null) {
                        if (tempPin == null ||tempPin.trim().isEmpty()) {
                            tempPin = pin;
                            showError(getString(R.string.Enter_again));
                            digits.clear();
                            updatePinView();
                        } else if (tempPin.equals(pin)) {
                            updateProfilePinOnServer(tempPin);
                        } else {
                            tempPin = null;
                            showError(getString(R.string.The_PIN_codes_do_not_match) + " " + getString(R.string.Enter_again));
                            digits.clear();
                            updatePinView();
                        }
                        showProgress(false);
                    } else {
                        if (savedPin.equals(pin)) {
                            requireActivity().runOnUiThread(() -> {
                                showProgress(false);
                                NavHostFragment.findNavController(PinEntryFragment.this)
                                        .navigate(R.id.action_pin_to_home);
                            });
                        } else {
                            count++;
                            showError(getString(R.string.Invalid_PIN_Attempts_remaining) + " " + count + "/3");
                            vibrate(200);
                            digits.clear();
                            updatePinView();
                        }
                    }
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
                    String tempMail = prefs.getUserEmail();
                    prefs.clearAll();
                    prefs.saveUserEmail(tempMail);
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
        for (int i = 0; i < digitButtons.length; i++) {
            btnDigits.get(i).setEnabled(!show);
        }
    }

    private void showError(String message) {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            errorText.setVisibility(View.VISIBLE);
            errorText.setText(message);
        });
    }

    private void updatePinView() {
        for (int i = 0; i < 4; i++) {
            pinViews.get(i).setBackgroundResource(i < digits.size() ?
                    R.drawable.pin_dot_filled : R.drawable.pin_dot_background);
        }
    }

    private void vibrate(long ms) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(ms);
            }
        }
    }
    private void hideError() {
        requireActivity().runOnUiThread(() -> errorText.setVisibility(View.GONE));
    }
}

package com.sigma.openfashion.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.sigma.openfashion.Validator;
import com.sigma.openfashion.data.SupabaseService;
import com.sigma.openfashion.ui.BaseFragment;
import com.sigma.openfashion.ui.HeaderConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * ResetPasswordFragment: ввод нового пароля после проверки OTP.
 */
public class ResetPasswordFragment extends BaseFragment {

    private TextInputEditText newPasswordEditText, confirmNewPasswordEditText;
    private MaterialButton updatePasswordButton;
    private MaterialTextView errorText;
    private ProgressBar resetProgress;

    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;

    public ResetPasswordFragment() {
        super(R.layout.fragment_reset_password);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reset_password, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        setHeaderVisibility(true);
        setupHeader(HeaderConfig.LOGO_ONLY);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        newPasswordEditText       = view.findViewById(R.id.newPasswordEditText);
        confirmNewPasswordEditText= view.findViewById(R.id.confirmNewPasswordEditText);
        updatePasswordButton      = view.findViewById(R.id.updatePasswordButton);
        errorText                 = view.findViewById(R.id.resetErrorText);
        resetProgress             = view.findViewById(R.id.resetProgress);

        prefs                     = SharedPrefHelper.getInstance(requireContext());

        supabaseService = new SupabaseService();
        Bundle args = getArguments();
        if (args != null) {
            String accessToken = args.getString("access_token");
            supabaseService.setJwtToken(accessToken);
        }

        updatePasswordButton.setOnClickListener(v -> attemptResetPassword());
    }

    private void attemptResetPassword() {
        String newPass = newPasswordEditText.getText() != null ? newPasswordEditText.getText().toString().trim() : "";
        String confirm = confirmNewPasswordEditText.getText() != null ? confirmNewPasswordEditText.getText().toString().trim() : "";

        if (!Validator.validatePassword(newPass, requireContext())) {
            newPasswordEditText.setError(getString(R.string.Incorrect_password_format));
            return;
        }
        if (!Validator.validatePassword(confirm, requireContext())) {
            confirmNewPasswordEditText.setError(getString(R.string.Incorrect_password_format));
            return;
        }
        if (!newPass.equals(confirm)) {
            newPasswordEditText.setError(getString(R.string.The_passwords_dont_match));
            confirmNewPasswordEditText.setError(getString(R.string.The_passwords_dont_match));
            return;
        }
        hideError();
        showProgress(true);

        supabaseService.resetPassword(newPass, new SupabaseService.QueryCallback() {
            @Override public void onSuccess(String jsonResponse) { handleResetSuccess(jsonResponse); }
            @Override public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void handleResetSuccess(String jsonResponse) {
        requireActivity().runOnUiThread(() -> {
            try {
                prefs.clearAll();
                JSONObject obj       = new JSONObject(jsonResponse);
                prefs.saveUserEmail(obj.getString("email"));
                showProgress(false);
                NavHostFragment.findNavController(ResetPasswordFragment.this)
                        .navigate(R.id.action_resetPassword_to_auth);
            } catch (JSONException e) {
                showError(getString(R.string.Parse_Error) + e.getMessage());
            }
        });
    }

    private void showProgress(boolean show) {
        resetProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        updatePasswordButton.setEnabled(!show);
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

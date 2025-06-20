package com.sigma.openfashion.ui.reg;

import android.os.Bundle;
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

import org.json.JSONException;
import org.json.JSONObject;

public class AccountRegisterFragment extends BaseFragment {
    private TextInputEditText usernameEditText, signUpAddressEditText, signUpPhoneEditText;
    private MaterialTextView errorText;
    private ProgressBar progressBar;
    private MaterialButton createAccountButton;

    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;

    public AccountRegisterFragment() {
        super(R.layout.fragment_account_register);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        usernameEditText      = view.findViewById(R.id.usernameEditText);
        signUpAddressEditText = view.findViewById(R.id.signUpAddressEditText);
        signUpPhoneEditText   = view.findViewById(R.id.signUpPhoneEditText);
        createAccountButton   = view.findViewById(R.id.createAccountButton);
        errorText             = view.findViewById(R.id.signUpErrorText);
        progressBar           = view.findViewById(R.id.signUpProgress);

        prefs                 = SharedPrefHelper.getInstance(requireContext());
        supabaseService       = new SupabaseService();

        Bundle args = getArguments();
        String json = args.getString("json", null);
        if (json == null) {
            runOnUi(this::navigateToAuthEntry);
            return;
        }

        try {
            JSONObject obj       = new JSONObject(json);
            String accessToken   = obj.optString("access_token", null);
            String refresh_token = obj.optString("refresh_token", null);
            JSONObject userObj   = obj.optJSONObject("user");
            String userId        = userObj != null ? userObj.optString("id", null) : null;

            prefs.saveJwtToken(accessToken);
            prefs.saveRefreshToken(refresh_token);
            prefs.saveUserId(userId);

            supabaseService.setJwtToken(accessToken);
        } catch (JSONException e) {
            showError(e.getMessage());
        }

        createAccountButton.setOnClickListener(v -> createProfile());
    }

    private void createProfile() {
        String name = usernameEditText.getText() != null ? usernameEditText.getText().toString().trim() : "";
        String postMail = signUpAddressEditText.getText() != null ? signUpAddressEditText.getText().toString().trim() : "";
        String phone = signUpPhoneEditText.getText() != null ? signUpPhoneEditText.getText().toString().trim() : "";

        if (!Validator.validateUsername(name, requireContext())) {
            usernameEditText.setError(getString(R.string.Incorrect_name_format));
            return;
        }
        if (postMail.trim().isEmpty()) {
            signUpAddressEditText.setError(getString(R.string.Incorrect_address_format));
            return;
        }
        if (!Validator.validatePhone(phone, requireContext())) {
            signUpPhoneEditText.setError(getString(R.string.Incorrect_phone_format));
            return;
        }
        hideError();
        showProgress(true);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        createAccountButton.setEnabled(!show);
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

    private void navigateToAuthEntry() {
        NavHostFragment.findNavController(AccountRegisterFragment.this)
                .navigate(R.id.action_reg_to_auth);
    }

    private void navigateToPinEntry() {
        NavHostFragment.findNavController(AccountRegisterFragment.this)
                .navigate(R.id.action_reg_to_pin);
    }
}
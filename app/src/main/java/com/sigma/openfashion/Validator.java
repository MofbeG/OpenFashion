package com.sigma.openfashion;

import android.content.Context;
import android.widget.Toast;

public class Validator {
    public static boolean validateEmail(String email, Context context) {
        if (email.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.Enter_your_email_address), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || !email.matches("^[a-z0-9]+(\\.[a-z0-9]+)*@[a-z0-9]+([-][a-z0-9]+)*(\\.[a-z]{2,})+$")) {
            Toast.makeText(context, context.getString(R.string.Incorrect_email_format), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public static boolean validatePhone(String phone, Context context) {
        if (phone.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.Enter_the_phone_number), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!phone.matches("^\\+?[0-9\\s\\-\\(\\)]{7,15}$")) {
            Toast.makeText(context, context.getString(R.string.Incorrect_phone_format), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public static boolean validateUsername(String username, Context context) {
        if (username.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.Enter_your_user_name), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!username.matches("^[a-zA-Z0-9_-]{3,20}$")) {
            Toast.makeText(context, context.getString(R.string.The_username_must_be_), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public static boolean validatePassword(String passoword, Context context) {
        if (passoword.isEmpty() || passoword.length() > 8) {
            Toast.makeText(context, context.getString(R.string.Enter_password), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}

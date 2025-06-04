package com.sigma.openfashion;

import android.content.Context;
import android.widget.Toast;

public class Validator {
    public static boolean validateEmail(String email, Context context) {
        if (email.isEmpty()) {
            Toast.makeText(context, "Введите адрес электронной почты", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Стандартная проверка через Android-паттерн
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(context, "Неверный формат электронной почты", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!email.matches("^[a-z0-9]+(\\.[a-z0-9]+)*@[a-z0-9]+([-][a-z0-9]+)*(\\.[a-z]{2,})+$")) {
            Toast.makeText(context, "Неверный формат электронной почты", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public static boolean validatePhone(String phone, Context context) {
        if (phone.isEmpty()) {
            Toast.makeText(context, "Введите номер телефона", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!phone.matches("^\\+?[0-9\\s\\-\\(\\)]{7,15}$")) {
            Toast.makeText(context, "Неверный формат телефона", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public static boolean validateUsername(String username, Context context) {
        if (username.isEmpty()) {
            Toast.makeText(context, "Введите имя пользователя", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!username.matches("^[a-zA-Z0-9_-]{3,20}$")) {
            Toast.makeText(context, "Имя пользователя должно содержать 3–20 символов", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public static boolean validatePassword(String passoword, Context context) {
        if (passoword.isEmpty() || passoword.length() > 8) {
            Toast.makeText(context, "Введите пароль", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}

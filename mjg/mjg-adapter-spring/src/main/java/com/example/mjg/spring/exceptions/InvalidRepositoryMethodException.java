package com.example.mjg.spring.exceptions;

import com.example.mjg.exceptions.BaseMigrationException;

import java.lang.reflect.Method;

public class InvalidRepositoryMethodException
extends BaseMigrationException {
    public InvalidRepositoryMethodException(Method method, String details) {
        super("Invalid repository method: " + method + "\nDetails: " + details);
    }

    public InvalidRepositoryMethodException(Method method, Throwable cause) {
        super("Invalid repository method: " + method, cause);
    }
}

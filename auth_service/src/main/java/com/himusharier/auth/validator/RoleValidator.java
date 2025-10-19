package com.himusharier.auth.validator;

import com.himusharier.auth.annotation.ValidRole;
import com.himusharier.auth.constants.UserRole;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RoleValidator implements ConstraintValidator<ValidRole, UserRole> {
    @Override
    public boolean isValid(UserRole userRole, ConstraintValidatorContext context) {
        if (userRole == null) {
            return false;
        }
        try {
            UserRole.valueOf(userRole.name());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

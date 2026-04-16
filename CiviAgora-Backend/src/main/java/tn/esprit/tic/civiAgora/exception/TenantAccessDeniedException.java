package tn.esprit.tic.civiAgora.exception;

public class TenantAccessDeniedException extends RuntimeException {

    public TenantAccessDeniedException(String message) {
        super(message);
    }
}

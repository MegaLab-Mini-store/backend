package kg.megalab.ministorebackend.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() { super("Неверный логин или пароль"); }
}

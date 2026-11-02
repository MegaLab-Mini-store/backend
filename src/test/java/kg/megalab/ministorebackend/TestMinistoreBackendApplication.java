package kg.megalab.ministorebackend;

import org.springframework.boot.SpringApplication;

public class TestMinistoreBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(MinistoreBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
